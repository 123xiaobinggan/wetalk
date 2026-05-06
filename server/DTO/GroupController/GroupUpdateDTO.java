package com.wetalk.DTO.GroupController;

import com.wetalk.model.Group;
import lombok.Data;

@Data
public class GroupUpdateDTO {
    private Group group;
    private Long updaterUserId;

    public GroupUpdateDTO() {
    }

    public GroupUpdateDTO(Group group, Long updaterUserId) {
        this.group = group;
        this.updaterUserId = updaterUserId;
    }

}
