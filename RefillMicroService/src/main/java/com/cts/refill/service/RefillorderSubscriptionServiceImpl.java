/**
 * 
 */
package com.cts.refill.service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cts.refill.exception.InvalidTokenException;
import com.cts.refill.feign.AuthFeign;
import com.cts.refill.model.RefillOrderLine;
import com.cts.refill.repo.RefillOrderLineRepository;

import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class RefillorderSubscriptionServiceImpl implements IRefillOrderSubscription{

	@Autowired
	private RefillOrderLineRepository lrepo;
	
	@Autowired
	private AuthFeign authFeign;
	
	@Value("${Exception.message}")
	private String msg;
	//private String msg = "Invalid Credentials";
	
	@Override
	public List<RefillOrderLine> getAllSubscription(String token) throws InvalidTokenException {
		log.info("inside getAllSubscription");
		if(authFeign.getValidity(token).isValid())
			return lrepo.findAll();
		else
			throw new InvalidTokenException(msg);
		
	}



	
	
	@Override
	public RefillOrderLine updateRefillOrderSubscription(long subId, String memberId, int quantity, int time,
			String token) throws InvalidTokenException {
		// start a refill for new subscriptions
		log.info("inside UpdateRefillOrderSubscription method");

		if (authFeign.getValidity(token).isValid()) {
			RefillOrderLine refillOrderSubscription = new RefillOrderLine();
			refillOrderSubscription.setSubID(subId);
			refillOrderSubscription.setRefillTime(time);
			refillOrderSubscription.setQuantity(quantity);
			refillOrderSubscription.setMemberID(memberId);
			/*DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");  
			String strDate = dateFormat.format(date);  
			LocalDate date1= LocalDate.parse(strDate).plusDays(time);
			Date date2 = Date.from(date1.atStartOfDay(ZoneId.systemDefault()).toInstant());
			refillOrderSubscription.setDate(date2);*/
			lrepo.save(refillOrderSubscription);

			return refillOrderSubscription;

		} else
			throw new InvalidTokenException(msg);
	}



	@Override
	public void deleteBySubscriptionId(long subID,String token) throws InvalidTokenException {
		log.info("inside deleteBySubscriptionId");
		if(authFeign.getValidity(token).isValid())
			lrepo.deleteBySubscriptionID(subID);
		else
			throw new InvalidTokenException(msg);
		
		
	}
	@Override
	public boolean getById(long subID,String token) throws InvalidTokenException {
		log.info("inside getById");
		if(authFeign.getValidity(token).isValid())
		{
			if(lrepo.findById(subID).isPresent())
			return true;
			else 
				return false;
		}
		
			//throw new InvalidTokenException(msg);
		return false;
	}

}
