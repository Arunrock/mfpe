package com.mailorderpharmacy.subscription.services;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.mailorderpharmacy.subscription.clients.AuthFeign;
import com.mailorderpharmacy.subscription.clients.DrugDetailClient;
import com.mailorderpharmacy.subscription.clients.RefillClient;
import com.mailorderpharmacy.subscription.controller.SubscriptionController;
import com.mailorderpharmacy.subscription.entity.PrescriptionDetails;
import com.mailorderpharmacy.subscription.entity.SubscriptionDetails;
import com.mailorderpharmacy.subscription.entity.TokenValid;
import com.mailorderpharmacy.subscription.exceptions.InvalidTokenException;
import com.mailorderpharmacy.subscription.exceptions.SubscriptionListEmptyException;
import com.mailorderpharmacy.subscription.repository.PrescriptionRepository;
import com.mailorderpharmacy.subscription.repository.SubscriptionRepository;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

/** Service Implementation class */

@Slf4j
@Service
public class SubscriptionServiceImpl implements SubscriptionService {
	
	

	//drug detail client
	@Autowired
	private DrugDetailClient drugDetailClient;
	
	// refill client
	@Autowired
	private RefillClient refillClient;

	//auth feign
	@Autowired
	private AuthFeign authFeign;

	//Prescription repository object
	@Autowired
	PrescriptionRepository prescriptionRepo;
	
	//Subscription repository object
	@Autowired
	SubscriptionRepository subscriptionRepo;


	String msg = "Invalid Credentials";

	/**
	 * @param prescriptionDetails
	 * @param token
	 * @return
	 * @throws Exception 
	 */
	@Override
	public ResponseEntity<String> subscribe(PrescriptionDetails prescriptionDetail, String token)  {
		log.info("Inside subscribe service method");
		if(authFeign.getValidity(token).getBody() !=null) {
			TokenValid tokenValid = authFeign.getValidity(token).getBody();
			if (tokenValid != null && tokenValid.isValid()) {
				log.info("subscribe service method- token is valid");
				drugDetailClient.getDrugByName(token, prescriptionDetail.getDrugName());
				
				ResponseEntity<Object> updateDetail = drugDetailClient.updateQuantity(token,prescriptionDetail.getDrugName(), prescriptionDetail.getMemberLocation(), prescriptionDetail.getQuantity());
				
				if(updateDetail.getStatusCodeValue()==200)
				{
				
				PrescriptionDetails prescriptionDetails = prescriptionRepo.save(prescriptionDetail);
				log.info("prescription saved");
				
				SubscriptionDetails subscriptionDetail = new SubscriptionDetails(prescriptionDetails.getPrescriptionId(),
						prescriptionDetails.getCourseDuration(), prescriptionDetails.getQuantity(),
						prescriptionDetails.getMemberId(), LocalDate.now(), prescriptionDetails.getMemberLocation(),
						"active", prescriptionDetails.getDrugName());

				log.info("subs obj created");

				SubscriptionDetails subscriptionDetails = subscriptionRepo.save(subscriptionDetail);
				// inform refill service to start refilling for this subscription
				//Date date = Date.from(subscriptionDetails.getSubscriptionDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
				refillClient.requestRefillSubscription(token, subscriptionDetails.getSubscriptionId(),
						subscriptionDetails.getMemberId(), subscriptionDetails.getQuantity(),
						subscriptionDetails.getRefillCycle());

				log.info("subs obj saved");
				}
				else {
					return new ResponseEntity<>("You have succesfully subscribed to " + prescriptionDetail.getDrugName(),
							HttpStatus.INSUFFICIENT_STORAGE);
				}
					
			} else
				throw new InvalidTokenException(msg);
		}

		return new ResponseEntity<>("You have succesfully subscribed to " + prescriptionDetail.getDrugName(),
				HttpStatus.OK);
	}

	/**
	 * @param mId
	 * @param sId
	 * @param token
	 * @return
	 * @throws InvalidTokenException
	 */
	@Override
	public ResponseEntity<String> unsubscribe(String mId, Long sId, String token)
			throws InvalidTokenException, FeignException {

		if(authFeign.getValidity(token).getBody() !=null) {
			TokenValid tokenValid = authFeign.getValidity(token).getBody();
			if (tokenValid != null && tokenValid.isValid()) {
				if(refillClient.getById(token, sId))
				{
				if (!refillClient.isPendingPaymentDues(token, sId)) {
					log.info("payment not clear");
					return new ResponseEntity<>("You have to clear your payment dues first.", HttpStatus.OK);
				}
				}
				SubscriptionDetails ss= subscriptionRepo.findById(sId).get();
				ResponseEntity<?> updateDetail = drugDetailClient.AddupdateQuantity(token,ss.getDrugName(), ss.getMemberLocation(), ss.getQuantity());
			    if(updateDetail.getStatusCodeValue()==200)
			    {
				subscriptionRepo.deleteById(sId);
				log.info("deleted ");
				// inform refill service to stop refilling for this sId
				refillClient.deleteRefillData(token, sId);
				log.info("delete refill success");
			    }
			} else
				throw new InvalidTokenException(msg);
		}

		return new ResponseEntity<>("You have succesfully Unsubscribed", HttpStatus.OK);
	}

	/**
	 * @param mId
	 * @param token
	 * @return
	 * @throws InvalidTokenException
	 */
	@Override
	public List<SubscriptionDetails> getAllSubscriptions(String mId, String token)
			throws InvalidTokenException, SubscriptionListEmptyException {
		// gets a list  of subscriptions for a given member id
		log.info("get subscription for ");

			if (authFeign.getValidity(token).getBody().isValid()) {
				if (subscriptionRepo.findByMemberId(mId).isEmpty())
					throw new SubscriptionListEmptyException("Currently you do not have any subscriptions");
				return subscriptionRepo.findByMemberId(mId);
			} else
				throw new InvalidTokenException(msg);


	}

	/**
	 * @param sId
	 * @param token
	 * @return
	 * @throws InvalidTokenException
	 */
	@Override
	public ResponseEntity<String> getDrugNameBySubscriptionId(Long sId, String token) throws InvalidTokenException {
		// extracts Drug name for given subscription Id
		log.info("getDrugNameBySubscriptionId");
		String drugName = null;
		String refillcycle=null;
		String date=null;
		SubscriptionDetails sd=null;
		 DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
		if(authFeign.getValidity(token).getBody() !=null) {
			TokenValid tokenValid = authFeign.getValidity(token).getBody();
			if (tokenValid != null && tokenValid.isValid()) {
				 sd = subscriptionRepo.findById(sId).get();
						
				/*refillcycle = String.valueOf(subscriptionRepo.findById(sId)
						.orElseThrow(() -> new SubscriptionListEmptyException("Refillcycle not found")).getRefillCycle());
				date = dateFormat.format(subscriptionRepo.findById(sId)
						.orElseThrow(() -> new SubscriptionListEmptyException("")).getSubscriptionDate());
			*/}
			else
				throw new InvalidTokenException(msg);
		}

		
	//return new ResponseEntity<>(drugName+","+refillcycle+","+date, HttpStatus.OK);
		//return new ResponseEntity<>(drugName+","+refillcycle, HttpStatus.OK);
		return new ResponseEntity<>(sd.getDrugName()+" "+sd.getRefillCycle()+" "+sd.getSubscriptionDate(), HttpStatus.OK);
	}
	
}
