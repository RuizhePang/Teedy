package com.sismics.docs.core.dao;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.sismics.docs.core.constant.AuditLogType;
import com.sismics.docs.core.constant.Constants;
import com.sismics.docs.core.model.jpa.RegisterRequest;
import com.sismics.docs.core.util.AuditLogUtil;
import com.sismics.util.context.ThreadLocalContext;

import at.favre.lib.crypto.bcrypt.BCrypt;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

/**
 * Register Request DAO
 *
 * @author Ruizhe PANG
 */
public class RegisterRequestDao {
    /**
     * Logger
     */
    private static final Logger log = LoggerFactory.getLogger(RegisterRequestDao.class);

    /**
     * Authenticate an register request
     */
    public RegisterRequest authenticate(String username, String password) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        Query q1 = em.createQuery("select r from RegisterRequest r where r.username = :username and r.deleteDate is null");
        Query q2 = em.createQuery("select u from User u where u.username = :username and u.deleteDate is null");
        q1.setParameter("username", username);
        q2.setParameter("username", username);
        List<?> l1 = q1.getResultList();
        if (l1.size() == 0) {
            return null;
        }
        RegisterRequest registerRequest = (RegisterRequest) l1.get(0);
        if (BCrypt.verifyer().verify(password.toCharArray(), registerRequest.getPassword()).verified) {
            return registerRequest;
        } else {
            return null;
        }
    }
    
    /**
     * Creates a new Register Request
     *
     */
    public String createRegisterRequest(RegisterRequest registerRequest) throws Exception {
        registerRequest.setId(UUID.randomUUID().toString());

        EntityManager em = ThreadLocalContext.get().getEntityManager();
        // Query q = em.createQuery("select r from RegisterRequest r where r.username = :username and r.deletedate is null");
        Query q = em.createQuery("select r from RegisterRequest r where r.username = :username");
        q.setParameter("username", registerRequest.getUsername());
        List<?> l = q.getResultList();
        if (l.size() > 0) {
            throw new Exception("AlreadyExistingUsername");
        }
        
        registerRequest.setCreateDate(new Date());
        registerRequest.setStatus(Constants.REGISTER_REQUEST_STATUS_PENDING);
        registerRequest.setPassword(hashPassword(registerRequest.getPassword()));
        em.persist(registerRequest);

        AuditLogUtil.create(registerRequest, AuditLogType.CREATE, registerRequest.getId());

        return registerRequest.getId();
    }


    /**
     * Hash the user's password.
     * 
     * @param password Clear password
     * @return Hashed password
     */
    private String hashPassword(String password) {
        int bcryptWork = Constants.DEFAULT_BCRYPT_WORK;
        String envBcryptWork = System.getenv(Constants.BCRYPT_WORK_ENV);
        if (!Strings.isNullOrEmpty(envBcryptWork)) {
            try {
                int envBcryptWorkInt = Integer.parseInt(envBcryptWork);
                if (envBcryptWorkInt >= 4 && envBcryptWorkInt <= 31) {
                    bcryptWork = envBcryptWorkInt;
                } else {
                    log.warn(Constants.BCRYPT_WORK_ENV + " needs to be in range 4...31. Falling back to " + Constants.DEFAULT_BCRYPT_WORK + ".");
                }
            } catch (NumberFormatException e) {
                log.warn(Constants.BCRYPT_WORK_ENV + " needs to be a number in range 4...31. Falling back to " + Constants.DEFAULT_BCRYPT_WORK + ".");
            }
        }
        return BCrypt.withDefaults().hashToString(bcryptWork, password.toCharArray());
    }
}
