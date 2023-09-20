package com.ledikom.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString
@Entity
public class Poll {

    @Transient
    public static boolean IS_ANONYMOUS = true;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String question;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "poll_option", joinColumns = @JoinColumn(name = "poll_id"))
    private List<PollOption> options;
    private Integer totalVoterCount;
    private String type;
    private Boolean allowMultipleAnswers;
    private Integer correctOptionId;
    private String explanation;
    private LocalDateTime lastVoteTimestamp;

    public Poll(final String question, final List<PollOption> options, final Integer totalVoterCount, final String type, final Boolean allowMultipleAnswers, final Integer correctOptionId, final String explanation) {
        this.question = question;
        this.options = options;
        this.totalVoterCount = totalVoterCount;
        this.type = type;
        this.allowMultipleAnswers = allowMultipleAnswers;
        this.correctOptionId = correctOptionId;
        this.explanation = explanation;
    }
}
