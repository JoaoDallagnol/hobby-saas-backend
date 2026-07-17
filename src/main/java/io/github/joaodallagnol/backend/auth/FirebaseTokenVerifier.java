package io.github.joaodallagnol.backend.auth;

public interface FirebaseTokenVerifier {

    FirebaseVerifiedToken verify(String idToken);
}
