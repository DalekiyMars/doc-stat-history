package com.ITQGroup.doc_stat_history.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
@Table(name = "approval_registry", schema = "entity_tables")
@Getter
@Setter
public class ApprovalRegistry {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "registry_seq")
    @SequenceGenerator(name = "registry_seq",
            sequenceName = "entity_tables.approval_registry_id_seq")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false, unique = true)
    private Document document;

    @Column(name = "approved_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime approvedAt;

    @Column(name = "initiator", nullable = false, length = 100)
    private String initiator;
}