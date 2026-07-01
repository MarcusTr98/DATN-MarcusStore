package com.fpoly.marcusstore.dto.request;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserPermissionRequest {
     private List<Integer> permissionIds;
}
