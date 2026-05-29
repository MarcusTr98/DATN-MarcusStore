package com.fpoly.marcusstore.repository.cms;

import com.fpoly.marcusstore.entity.cms.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> { // Khóa chính là String
    List<SystemSetting> findBySettingGroup(String groupName);
}