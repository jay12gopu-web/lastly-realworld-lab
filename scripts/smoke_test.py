#!/usr/bin/env python3
"""Authenticated smoke test against local dev/staging or remote read-only public demo."""
import argparse, json, sys, uuid
from urllib.parse import urlparse
from urllib.request import Request, urlopen

def request(base, method, path, payload=None, token=None):
    data = json.dumps(payload).encode() if payload is not None else None
    headers = {'Content-Type':'application/json'}
    if token: headers['Authorization']='Bearer '+token
    req=Request(base.rstrip('/')+path, data=data, method=method, headers=headers)
    with urlopen(req,timeout=15) as resp:
        raw=resp.read().decode()
        return resp.status,(json.loads(raw) if raw else None)

def main():
    parser=argparse.ArgumentParser(); parser.add_argument('--url',default='http://localhost:8080')
    args=parser.parse_args(); base=args.url.rstrip('/')
    host=urlparse(base).hostname
    try:
        _,cfg=request(base,'GET','/api/config')
        _,products=request(base,'GET','/api/products')
        print(f"PASS catalogue: {len(products)} products; writes enabled={cfg['ordersEnabled']}")
        if host not in ('localhost','127.0.0.1','::1'):
            assert cfg['ordersEnabled'] is False,'Remote public demo MUST be read-only'
            print('PASS public read-only check'); return 0
        if not cfg['ordersEnabled']:
            print('PASS read-only check'); return 0
        email='smoke-'+str(uuid.uuid4())+'@example.test'; password='RandomExamplePassword123!'
        request(base,'POST','/api/auth/register',{'email':email,'password':password})
        _,auth=request(base,'POST','/api/auth/login',{'email':email,'password':password})
        token=auth['accessToken']
        print('PASS registration + JWT login')
        product=next((p for p in products if p['stockQuantity']>0),None)
        if not product: print('SKIP order write: no stock'); return 0
        payload={'productId':product['id'],'quantity':1,'idempotencyKey':'smoke-'+str(uuid.uuid4())}
        _,first=request(base,'POST','/api/orders',payload,token)
        _,again=request(base,'POST','/api/orders',payload,token)
        assert first['id']==again['id'],'Idempotency failure'
        _,updated=request(base,'GET','/api/products')
        after=next(p for p in updated if p['id']==product['id'])
        assert after['stockQuantity']==product['stockQuantity']-1,'Stock mismatch'
        print('PASS authenticated order + idempotent retry + inventory'); return 0
    except Exception as ex:
        print('FAIL smoke test:',ex,file=sys.stderr); return 1
if __name__=='__main__':sys.exit(main())
