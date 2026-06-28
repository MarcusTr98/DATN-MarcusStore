package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.response.VoucherResponse;

import java.util.List;

public interface UserVoucherService {

    List<VoucherResponse> getAvailableVouchersForUser();

    List<VoucherResponse> getUserVouchersByVoucherId(Integer voucherId);

    void assignVoucherToUsers(Integer voucherId, List<Integer> userIds);

    void reassignVoucherUsers(Integer voucherId, List<Integer> newUserIds);
}
