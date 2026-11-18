#!/usr/bin/env python3
"""Local-only concurrency exercise; consumes disposable dev inventory."""
import json, sys, uuid
from concurrent.futures import ThreadPoolExecutor
from urllib.error import HTTPError
from urllib.request import Request,urlopen
BASE='http://127.0.0.1:8080' # local only
def call(method,path,payload=None,token=None):
    headers={'Content-Type':'application/json'}
    if token: headers['Authorization']='Bearer '+token
    req=Request(BASE+path,method=method,data=json.dumps(payload).encode() if payload is not None else None,headers=headers)
    try:
        with urlopen(req,timeout=15) as res: return res.status,json.loads(res.read())
    except HTTPError as ex: return ex.code,json.loads(ex.read())
def main():
    _,cfg=call('GET','/api/config')
    if not cfg.get('ordersEnabled'): print('Writes disabled'); return 1
    email='race-'+str(uuid.uuid4())+'@example.test'; password='RandomExamplePassword123!'
    assert call('POST','/api/auth/register',{'email':email,'password':password})[0]==201
    status,auth=call('POST','/api/auth/login',{'email':email,'password':password})
    assert status==200
    token=auth['accessToken']
    _,products=call('GET','/api/products')
    product=next((p for p in products if p['stockQuantity']>0),None)
    if not product: print('No stock to test'); return 1
    before=product['stockQuantity']; attempts=min(before+4,30)
    def buy(_):
        return call('POST','/api/orders',{'productId':product['id'],'quantity':1,
            'idempotencyKey':'race-'+str(uuid.uuid4())},token)[0]
    with ThreadPoolExecutor(max_workers=min(attempts,16)) as pool:
        codes=list(pool.map(buy,range(attempts)))
    _,result=call('GET','/api/products')
    after=next(p['stockQuantity'] for p in result if p['id']==product['id'])
    wins=codes.count(201); conflicts=codes.count(409)
    print(f'Successful={wins}, rejected={conflicts}, stock={before}->{after}; codes={codes}')
    passed=after>=0 and before-after==wins and all(x in (201,409) for x in codes)
    print('PASS: inventory consistent' if passed else 'FAIL: check logs')
    return 0 if passed else 1
if __name__=='__main__':sys.exit(main())
