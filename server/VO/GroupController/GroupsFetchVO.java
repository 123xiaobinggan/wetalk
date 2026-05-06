package com.wetalk.VO.GroupController;

import com.wetalk.model.*;

public class GroupsFetchVO {
    private Group[] groups;

    public GroupsFetchVO(Group[] groups) {
        this.groups = groups;
    }

    public Group[] getGroups() {
        return groups;
    }

    public void setGroups(Group[] groups) {
        this.groups = groups;
    }
}
