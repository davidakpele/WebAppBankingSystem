package com.pesco.authentication.serviceImplementations;

import org.springframework.stereotype.Service;
import com.pesco.authentication.models.AuthorizeUserVerification;
import com.pesco.authentication.repositories.AuthorizeUserVerificationRepository;
import com.pesco.authentication.services.AuthorizeUserVerificationService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthorizeUserVerificationServiceImplementation implements AuthorizeUserVerificationService{

    private final AuthorizeUserVerificationRepository authorizeUserVerificationRepository;

    @Override
    public void save(Long userId, Long id) {
        AuthorizeUserVerification auth = new AuthorizeUserVerification();
        auth.setId(id);
        auth.setUserId(userId);
        authorizeUserVerificationRepository.save(auth);
    }

}
