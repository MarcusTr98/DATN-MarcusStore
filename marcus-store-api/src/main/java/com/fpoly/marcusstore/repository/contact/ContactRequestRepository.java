package com.fpoly.marcusstore.repository.contact;

import com.fpoly.marcusstore.entity.contact.ContactRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactRequestRepository extends JpaRepository<ContactRequest, Integer> {
}