/**
 * 
 */
package com.cts.refill.repo;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.cts.refill.model.RefillOrder;


//@Repository
public interface RefillOrderRepository extends JpaRepository<RefillOrder, Long>{

	@Query(value = "SELECT u.subID FROM RefillOrder u WHERE MEMBERID = ?1")
	 List<Long> findByMemberId(String mId);

	
}
