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
@Table("team")
public class Team implements Serializable {
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;
    private String name;
    @Column("ownerId")
    private Long ownerId;
    @Column("isPersonal")
    private Integer isPersonal;
    @Column("createTime")
    private LocalDateTime createTime;
    @Column("updateTime")
    private LocalDateTime updateTime;
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
