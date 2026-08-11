package com.eureka.flowbuilder.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Table("team_asset")
public class TeamAsset implements Serializable {
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;
    @Column("teamId")
    private Long teamId;
    @Column("uploaderId")
    private Long uploaderId;
    private String name;
    private String url;
    @Column("tosKey")
    private String tosKey;
    @Column("contentType")
    private String contentType;
    private Long size;
    private Integer width;
    private Integer height;
    private String tags;
    @Column("createTime")
    private LocalDateTime createTime;
    @Column("updateTime")
    private LocalDateTime updateTime;
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
