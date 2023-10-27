package com.ledikom.service;

import com.ledikom.model.Poll;
import com.ledikom.model.PollOption;
import com.ledikom.repository.PollRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class PollService {

    @Value("${poll.active-time-days}")
    private int activeTimeInDays;

    private final PollRepository pollRepository;

    public PollService(final PollRepository pollRepository) {
        this.pollRepository = pollRepository;
    }

    public Poll tgPollToLedikomPoll(final org.telegram.telegrambots.meta.api.objects.polls.Poll telegramPoll) {
        return new Poll(telegramPoll.getQuestion(),
                telegramPoll.getOptions().stream().map(tgPollOption -> new PollOption(tgPollOption.getText(), tgPollOption.getVoterCount())).collect(Collectors.toList()),
                telegramPoll.getTotalVoterCount(), telegramPoll.getType(),
                telegramPoll.getAllowMultipleAnswers(), telegramPoll.getCorrectOptionId(), telegramPoll.getExplanation());
    }

    public Poll savePoll(final Poll poll) {
        return pollRepository.save(poll);
    }

    public Poll findByQuestion(final String question) {
        return pollRepository.findByQuestion(question).orElseThrow(() -> new RuntimeException("Poll not found by question " + question));
    }

    private List<Poll> getActualPolls() {
        return pollRepository.findAll().stream().filter(poll -> poll.getLastVoteTimestamp().isAfter(LocalDateTime.now().minusDays(activeTimeInDays))).toList();
    }

    public String getPollsInfoForAdmin() {
        List<Poll> polls = getActualPolls();

        StringBuilder sb = new StringBuilder("Информация об опросах за последние " + activeTimeInDays + " дней:\n\n\n");

        if (polls.isEmpty()) {
            return sb.append("Список пуст").toString();
        }

        IntStream.range(0, polls.size())
                .forEach(index -> {
                    Poll poll = polls.get(index);
                    sb.append(index + 1).append(". ").append(poll.getQuestion()).append("\n");
                    sb.append("*").append(poll.getTotalVoterCount()).append("*").append(" - Количество опрошенных").append("\n");
                    poll.getOptions().forEach(option -> sb.append("*").append(option.getVoterCount()).append("*").append(" - ").append(option.getText()).append("\n"));
                    sb.append("\n\n\n");
                });
        return sb.toString();
    }
}
