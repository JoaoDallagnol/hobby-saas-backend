package io.github.joaodallagnol.backend.user;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_hobbies")
public class UserHobby {

    @EmbeddedId
    private UserHobbyId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private ProductUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("hobbyId")
    @JoinColumn(name = "hobby_id", nullable = false)
    private Hobby hobby;

    @Column(name = "experience_level")
    private String experienceLevel;

    protected UserHobby() {
    }

    public UserHobby(ProductUser user, Hobby hobby, String experienceLevel) {
        this.id = new UserHobbyId(user.getId(), hobby.getId());
        this.user = user;
        this.hobby = hobby;
        this.experienceLevel = experienceLevel;
    }

    public UserHobbyId getId() {
        return id;
    }

    public ProductUser getUser() {
        return user;
    }

    public Hobby getHobby() {
        return hobby;
    }

    public String getExperienceLevel() {
        return experienceLevel;
    }

    public void updateExperienceLevel(String experienceLevel) {
        this.experienceLevel = experienceLevel;
    }
}
