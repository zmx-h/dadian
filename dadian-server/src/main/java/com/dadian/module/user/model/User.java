package com.dadian.module.user.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("users")
public class User {

    @TableId(type = IdType.INPUT)
    private String id;

    private String phone;

    @TableField("phone_hash")
    private String phoneHash;

    @TableField("phone_encrypted")
    private byte[] phoneEncrypted;

    @TableField("display_name")
    private String displayName;

    @TableField("avatar_url")
    private String avatarUrl;

    private String bio;

    @TableField("social_trait")
    private Integer socialTrait;

    @TableField("weekend_style")
    private Integer weekendStyle;

    @TableField("crowd_feeling")
    private Integer crowdFeeling;

    @TableField("companion_tone")
    private String companionTone;

    @TableField("companion_intensity")
    private Integer companionIntensity;

    @TableField("humor_level")
    private Integer humorLevel;

    @TableField("achievement_visibility")
    private String achievementVisibility;

    @TableField("location_retention")
    private String locationRetention;

    @TableField("deleted_at")
    private OffsetDateTime deletedAt;

    @TableField("created_at")
    private OffsetDateTime createdAt;

    @TableField("updated_at")
    private OffsetDateTime updatedAt;
}
