package com.hermes.profile.exception;
public class ProfileNotFoundException extends RuntimeException { public ProfileNotFoundException(Long userId) { super("Profile not found for user " + userId); } }
