package com.wetalk.ws.Type.Group.Group_Update;

import com.wetalk.model.*;
import lombok.Data;

@Data
public class GroupUpdatePush {
    private Group group;

    public GroupUpdatePush() {
    }

    public GroupUpdatePush(Group group) {
        this.group = group;
    }
}
