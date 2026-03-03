package com.lsw_proto_cloud.core.persistence;

import java.util.List;
import java.util.Optional;

public interface UserProfileRepository {
    //CRUD OPERATIONS FOR USER PROFILES
    void saveUser(UserProfile userProfile);
    void deleteUserByName(String username);
    Optional<UserProfile> getUserByName(String username);
    List<UserProfile> getAllUsers();
    boolean exists(String username);
}
