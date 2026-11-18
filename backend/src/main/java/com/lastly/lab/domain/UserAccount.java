package com.lastly.lab.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="user_accounts")
public class UserAccount {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,unique=true,length=254) private String email;
    @Column(name="password_hash",nullable=false,length=100) private String passwordHash;
    @Column(nullable=false,length=32) private String role;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    protected UserAccount() {}
    public UserAccount(String email, String passwordHash) {
        this.email=email; this.passwordHash=passwordHash; this.role="CUSTOMER";
        this.createdAt=Instant.now();
    }
    public Long getId(){return id;}
    public String getEmail(){return email;}
    public String getPasswordHash(){return passwordHash;}
    public String getRole(){return role;}
}
