package com.benny1611.template.dao;

import com.benny1611.template.entity.UserOAuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserOAuthAccountRepository extends JpaRepository<UserOAuthAccount, Long> {
    Optional<UserOAuthAccount> findByProvider_NameIgnoreCaseAndProviderUserId(String providerName, String providerUserId);
}
