package com.teamof4.mogu.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;
import javax.validation.Valid;

import static javax.persistence.FetchType.LAZY;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostSkill extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Valid
    @JsonIgnore
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "project_study_id")
    private ProjectStudy projectStudy;

    @Valid
    @JsonIgnore
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "skill_id")
    private Skill skill;

}
