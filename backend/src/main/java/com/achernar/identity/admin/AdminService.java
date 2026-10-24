package com.achernar.identity.admin;
import com.achernar.identity.users.UserDto;
import java.util.*;
public interface AdminService {
    record Page(List<UserDto> items,int page,int size,long total,long totalPages) {}
    Page list(String query,int page,int size);
    UserDto get(UUID id);
    UserDto create(UUID actor,String name,String email,List<String> profiles);
    UserDto change(UUID actor,UUID target,String role,String status);
    UserDto edit(UUID actor,UUID target,String name,String email,List<String> profiles,String role,String status);
    void delete(UUID actor,UUID target);
}
