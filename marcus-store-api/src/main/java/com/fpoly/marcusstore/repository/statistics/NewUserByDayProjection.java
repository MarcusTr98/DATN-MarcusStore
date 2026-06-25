package com.fpoly.marcusstore.repository.statistics;

import java.sql.Date;

public interface NewUserByDayProjection {
    Date getRegisterDate();
    Long getTotalNewUsers();
}