package com.fpoly.marcusstore.dto.request;

import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserPermissionRequest {
     private List<Integer> permissionIds;
     
     // Nhận moduleNames thay vì permissionIds (frontend gửi cái này)
     private Set<String> moduleNames;
}
