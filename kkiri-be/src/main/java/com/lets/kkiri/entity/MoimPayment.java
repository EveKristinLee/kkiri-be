package com.lets.kkiri.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;

@Getter
@NoArgsConstructor
@Entity
public class MoimPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumns({
            @JoinColumn(name="member_id"),
            @JoinColumn(name="moim_id")
    })
    private MemberGroup memberGroup;
    @ColumnDefault("0")
    private Integer expenditure;
    @ColumnDefault("false")
    private Boolean isPaid;

    @Builder
    public MoimPayment(MemberGroup memberGroup, Integer expenditure) {
        this.memberGroup = memberGroup;
        this.expenditure = expenditure;
    }

    public void addExpenditure(int expenditure) {
        this.expenditure += expenditure;
    }

    public void pay() {
        this.isPaid = true;
    }
}
