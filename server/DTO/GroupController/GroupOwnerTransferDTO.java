package com.wetalk.DTO.GroupController;

import lombok.Data;

@Data
public class GroupOwnerTransferDTO {
    private Long groupId;
    private Long ownerUserId;
    private Long transfereeUserId;

    public GroupOwnerTransferDTO() {
    }

    public GroupOwnerTransferDTO(Long groupId, Long ownerUserId, Long transfereeUserId) {
        this.groupId = groupId;
        this.ownerUserId = ownerUserId;
        this.transfereeUserId = transfereeUserId;
    }
}
