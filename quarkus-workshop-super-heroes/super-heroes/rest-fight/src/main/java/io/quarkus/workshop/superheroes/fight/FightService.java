package io.quarkus.workshop.superheroes.fight;

import io.quarkus.workshop.superheroes.fight.client.Hero;
import io.quarkus.workshop.superheroes.fight.client.HeroService;
import io.quarkus.workshop.superheroes.fight.client.Villain;
import io.quarkus.workshop.superheroes.fight.client.VillainService;
import io.smallrye.reactive.messaging.annotations.Emitter;
import io.smallrye.reactive.messaging.annotations.OnOverflow;
import io.smallrye.reactive.messaging.annotations.Stream;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Random;

import static javax.transaction.Transactional.TxType.REQUIRED;
import static javax.transaction.Transactional.TxType.SUPPORTS;

@ApplicationScoped
@Transactional(SUPPORTS)
public class FightService {

    @Inject
    @RestClient
    HeroService heroService;

    @Inject
    @RestClient
    VillainService villainService;

    private final Random random = new Random();

    @Inject
    @Stream("fights-channel") Emitter<Fight> emitter;

    public List<Fight> findAllFights() {
        return Fight.listAll();
    }

    public Fight findFightById(Long id) {
        return Fight.findById(id);
    }

    @Transactional(REQUIRED)
    public Fight persistFight(Fighters fighters) {
        Fight fight;

        int heroAdjust = random.nextInt(20);
        int villainAdjust = random.nextInt(20);

        if ((fighters.getHero().getLevel() + heroAdjust)
            > (fighters.getVillain().getLevel() + villainAdjust)) {
            fight = heroWon(fighters);
        } else if (fighters.getHero().getLevel() < fighters.getVillain().getLevel()) {
            fight = villainWon(fighters);
        } else {
            fight = random.nextBoolean() ? heroWon(fighters) : villainWon(fighters);
        }

        fight.fightDate = Instant.now();
        Fight.persist(fight);
        emitter.send(fight);
        return fight;
    }

    private Fight heroWon(Fighters fighters) {
        Fight fight = new Fight();
        fight.winnerName = fighters.getHero().getName();
        fight.winnerPicture = fighters.getHero().getPicture();
        fight.winnerLevel = fighters.getHero().getLevel();
        fight.loserName = fighters.getVillain().getName();
        fight.loserPicture = fighters.getVillain().getPicture();
        fight.loserLevel = fighters.getVillain().getLevel();
        fight.winnerTeam = "heroes";
        fight.loserTeam = "villains";
        return fight;
    }

    private Fight villainWon(Fighters fighters) {
        Fight fight = new Fight();
        fight.winnerName = fighters.getVillain().getName();
        fight.winnerPicture = fighters.getVillain().getPicture();
        fight.winnerLevel = fighters.getVillain().getLevel();
        fight.loserName = fighters.getHero().getName();
        fight.loserPicture = fighters.getHero().getPicture();
        fight.loserLevel = fighters.getHero().getLevel();
        fight.winnerTeam = "villains";
        fight.loserTeam = "heroes";
        return fight;
    }

    Fighters findRandomFighters() {
        Hero hero = heroService.findRandomHero();
        Villain villain = villainService.findRandomVillain();
        Fighters fighters = new Fighters();
        fighters.setHero(hero);
        fighters.setVillain(villain);
        return fighters;
    }
}
