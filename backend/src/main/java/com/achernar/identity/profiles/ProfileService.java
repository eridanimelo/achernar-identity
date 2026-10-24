package com.achernar.identity.profiles;
import java.util.*;
public interface ProfileService {
    record Page(List<ProfileDto> items,int page,int size,long total,long totalPages){}
    Page list(String q,Boolean active,int page,int size);
    ProfileDto get(UUID id);
    ProfileDto create(UUID actor,String code,String name,String description);
    ProfileDto update(UUID actor,UUID id,String name,String description,boolean active);
    void delete(UUID actor,UUID id);
    List<UUID> resolve(List<String> codes);
}
