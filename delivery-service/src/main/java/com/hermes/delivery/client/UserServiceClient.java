package com.hermes.delivery.client;

import com.hermes.common.user.UserSummary;

import java.util.Optional;

public interface UserServiceClient {
    Optional<UserSummary> findUserByEmail(String email);
}