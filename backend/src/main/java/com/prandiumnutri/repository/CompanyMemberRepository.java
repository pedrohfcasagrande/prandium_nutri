package com.prandiumnutri.repository;

import com.prandiumnutri.domain.CompanyMember;
import com.prandiumnutri.domain.LinkStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompanyMemberRepository extends JpaRepository<CompanyMember, Long> {

    List<CompanyMember> findByCompanyIdOrderByInvitedAtDesc(Long companyId);

    List<CompanyMember> findByUserIdAndStatus(Long userId, LinkStatus status);

    List<CompanyMember> findByInvitedByEmailAndStatus(String email, LinkStatus status);

    Optional<CompanyMember> findByInviteToken(String token);

    @Query("""
           select count(m) > 0 from CompanyMember m
           where m.company.id = :companyId
             and m.user.id = :userId
             and m.status = 'ACTIVE'
           """)
    boolean isActiveMember(@Param("companyId") Long companyId, @Param("userId") Long userId);
}
