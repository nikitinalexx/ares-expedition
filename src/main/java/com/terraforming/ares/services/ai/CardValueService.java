package com.terraforming.ares.services.ai;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.PaymentValidationService;
import com.terraforming.ares.services.SpecialEffectsService;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Created by oleksii.nikitin
 * Creation date 25.11.2022
 */
@Service
public class CardValueService {
    private static final double MAX_PRIORITY = 10.0;
    private final Map<Integer, Double> cardToWeightFirstHalf = new HashMap<>();
    private final Map<Integer, Double> cardToWeightSecondHalf = new HashMap<>();
    private final CardService cardService;
    private final PaymentValidationService paymentValidationService;
    private final SpecialEffectsService specialEffectsService;

    public CardValueService(CardService cardService, PaymentValidationService paymentValidationService, SpecialEffectsService specialEffectsService) {
        this.cardService = cardService;
        this.paymentValidationService = paymentValidationService;
        this.specialEffectsService = specialEffectsService;
        cardToWeightFirstHalf.put(1, 40.0);
        cardToWeightFirstHalf.put(2, 57.142857142857146);
        cardToWeightFirstHalf.put(3, 51.04895104895105);
        cardToWeightFirstHalf.put(4, 80.76923076923077);
        cardToWeightFirstHalf.put(5, 0.0);
        cardToWeightFirstHalf.put(6, 90.625);
        cardToWeightFirstHalf.put(7, 0.0);
        cardToWeightFirstHalf.put(8, 0.0);
        cardToWeightFirstHalf.put(9, 44.44444444444444);
        cardToWeightFirstHalf.put(10, 41.007194244604314);
        cardToWeightFirstHalf.put(11, 48.611111111111114);
        cardToWeightFirstHalf.put(12, 0.0);
        cardToWeightFirstHalf.put(13, 57.93650793650794);
        cardToWeightFirstHalf.put(14, 0.0);
        cardToWeightFirstHalf.put(15, 50.74626865671642);
        cardToWeightFirstHalf.put(16, 0.0);
        cardToWeightFirstHalf.put(17, 49.78354978354978);
        cardToWeightFirstHalf.put(18, 100.0);
        cardToWeightFirstHalf.put(19, 0.0);
        cardToWeightFirstHalf.put(20, 0.0);
        cardToWeightFirstHalf.put(21, 0.0);
        cardToWeightFirstHalf.put(22, 55.88235294117647);
        cardToWeightFirstHalf.put(23, 54.95049504950495);
        cardToWeightFirstHalf.put(24, 0.0);
        cardToWeightFirstHalf.put(25, 63.63636363636363);
        cardToWeightFirstHalf.put(26, 65.40880503144655);
        cardToWeightFirstHalf.put(27, 0.0);
        cardToWeightFirstHalf.put(28, 0.0);
        cardToWeightFirstHalf.put(29, 0.0);
        cardToWeightFirstHalf.put(30, 55.55555555555556);
        cardToWeightFirstHalf.put(31, 52.54237288135593);
        cardToWeightFirstHalf.put(32, 0.0);
        cardToWeightFirstHalf.put(33, 70.0);
        cardToWeightFirstHalf.put(34, 40.0);
        cardToWeightFirstHalf.put(35, 61.81818181818182);
        cardToWeightFirstHalf.put(36, 67.9245283018868);
        cardToWeightFirstHalf.put(37, 67.63848396501457);
        cardToWeightFirstHalf.put(38, 52.72727272727273);
        cardToWeightFirstHalf.put(39, 0.0);
        cardToWeightFirstHalf.put(40, 61.198738170347006);
        cardToWeightFirstHalf.put(41, 0.0);
        cardToWeightFirstHalf.put(42, 63.98713826366559);
        cardToWeightFirstHalf.put(43, 0.0);
        cardToWeightFirstHalf.put(44, 65.29968454258675);
        cardToWeightFirstHalf.put(45, 55.85585585585586);
        cardToWeightFirstHalf.put(46, 82.6086956521739);
        cardToWeightFirstHalf.put(47, 47.05882352941177);
        cardToWeightFirstHalf.put(48, 65.18518518518519);
        cardToWeightFirstHalf.put(49, 59.85401459854015);
        cardToWeightFirstHalf.put(50, 0.0);
        cardToWeightFirstHalf.put(51, 63.192182410423456);
        cardToWeightFirstHalf.put(52, 62.22222222222222);
        cardToWeightFirstHalf.put(53, 61.07954545454545);
        cardToWeightFirstHalf.put(54, 59.183673469387756);
        cardToWeightFirstHalf.put(55, 45.36082474226804);
        cardToWeightFirstHalf.put(56, 43.646408839779006);
        cardToWeightFirstHalf.put(57, 0.0);
        cardToWeightFirstHalf.put(58, 47.61904761904762);
        cardToWeightFirstHalf.put(59, 59.79899497487437);
        cardToWeightFirstHalf.put(60, 71.30434782608695);
        cardToWeightFirstHalf.put(61, 42.10526315789474);
        cardToWeightFirstHalf.put(62, 50.3448275862069);
        cardToWeightFirstHalf.put(63, 47.019867549668874);
        cardToWeightFirstHalf.put(64, 39.130434782608695);
        cardToWeightFirstHalf.put(65, 0.0);
        cardToWeightFirstHalf.put(66, 0.0);
        cardToWeightFirstHalf.put(67, 70.49180327868852);
        cardToWeightFirstHalf.put(68, 0.0);
        cardToWeightFirstHalf.put(69, 63.02250803858521);
        cardToWeightFirstHalf.put(70, 65.79710144927536);
        cardToWeightFirstHalf.put(71, 0.0);
        cardToWeightFirstHalf.put(72, 53.20754716981132);
        cardToWeightFirstHalf.put(73, 53.37837837837838);
        cardToWeightFirstHalf.put(74, 59.05797101449275);
        cardToWeightFirstHalf.put(75, 59.375);
        cardToWeightFirstHalf.put(76, 52.40963855421687);
        cardToWeightFirstHalf.put(77, 58.13953488372093);
        cardToWeightFirstHalf.put(78, 51.65289256198347);
        cardToWeightFirstHalf.put(79, 0.0);
        cardToWeightFirstHalf.put(80, 58.51063829787234);
        cardToWeightFirstHalf.put(81, 58.898305084745765);
        cardToWeightFirstHalf.put(82, 78.125);
        cardToWeightFirstHalf.put(83, 61.53846153846154);
        cardToWeightFirstHalf.put(84, 60.588235294117645);
        cardToWeightFirstHalf.put(85, 64.77272727272727);
        cardToWeightFirstHalf.put(86, 0.0);
        cardToWeightFirstHalf.put(87, 49.09090909090909);
        cardToWeightFirstHalf.put(88, 52.459016393442624);
        cardToWeightFirstHalf.put(89, 68.72246696035242);
        cardToWeightFirstHalf.put(90, 0.0);
        cardToWeightFirstHalf.put(91, 46.42857142857143);
        cardToWeightFirstHalf.put(92, 0.0);
        cardToWeightFirstHalf.put(93, 40.229885057471265);
        cardToWeightFirstHalf.put(94, 85.41666666666667);
        cardToWeightFirstHalf.put(95, 48.57142857142857);
        cardToWeightFirstHalf.put(96, 64.09495548961425);
        cardToWeightFirstHalf.put(97, 53.75494071146245);
        cardToWeightFirstHalf.put(98, 50.476190476190474);
        cardToWeightFirstHalf.put(99, 64.91803278688525);
        cardToWeightFirstHalf.put(100, 50.0);
        cardToWeightFirstHalf.put(101, 59.130434782608695);
        cardToWeightFirstHalf.put(102, 48.78048780487805);
        cardToWeightFirstHalf.put(103, 66.55405405405405);
        cardToWeightFirstHalf.put(104, 0.0);
        cardToWeightFirstHalf.put(105, 0.0);
        cardToWeightFirstHalf.put(106, 0.0);
        cardToWeightFirstHalf.put(107, 37.5);
        cardToWeightFirstHalf.put(108, 48.148148148148145);
        cardToWeightFirstHalf.put(109, 37.234042553191486);
        cardToWeightFirstHalf.put(110, 49.6);
        cardToWeightFirstHalf.put(111, 0.0);
        cardToWeightFirstHalf.put(112, 43.63636363636363);
        cardToWeightFirstHalf.put(113, 64.24050632911393);
        cardToWeightFirstHalf.put(114, 48.214285714285715);
        cardToWeightFirstHalf.put(115, 0.0);
        cardToWeightFirstHalf.put(116, 67.10526315789474);
        cardToWeightFirstHalf.put(117, 66.66666666666667);
        cardToWeightFirstHalf.put(118, 0.0);
        cardToWeightFirstHalf.put(119, 58.87850467289719);
        cardToWeightFirstHalf.put(120, 53.333333333333336);
        cardToWeightFirstHalf.put(121, 0.0);
        cardToWeightFirstHalf.put(122, 65.44117647058823);
        cardToWeightFirstHalf.put(123, 60.816326530612244);
        cardToWeightFirstHalf.put(124, 35.483870967741936);
        cardToWeightFirstHalf.put(125, 30.76923076923077);
        cardToWeightFirstHalf.put(126, 50.93167701863354);
        cardToWeightFirstHalf.put(127, 0.0);
        cardToWeightFirstHalf.put(128, 0.0);
        cardToWeightFirstHalf.put(129, 57.24907063197026);
        cardToWeightFirstHalf.put(130, 11.764705882352942);
        cardToWeightFirstHalf.put(131, 62.096774193548384);
        cardToWeightFirstHalf.put(132, 0.0);
        cardToWeightFirstHalf.put(133, 0.0);
        cardToWeightFirstHalf.put(134, 0.0);
        cardToWeightFirstHalf.put(135, 0.0);
        cardToWeightFirstHalf.put(136, 60.66945606694561);
        cardToWeightFirstHalf.put(137, 69.79166666666667);
        cardToWeightFirstHalf.put(138, 59.375);
        cardToWeightFirstHalf.put(139, 76.62337662337663);
        cardToWeightFirstHalf.put(140, 39.516129032258064);
        cardToWeightFirstHalf.put(141, 44.52554744525548);
        cardToWeightFirstHalf.put(142, 0.0);
        cardToWeightFirstHalf.put(143, 27.272727272727273);
        cardToWeightFirstHalf.put(144, 0.0);
        cardToWeightFirstHalf.put(145, 0.0);
        cardToWeightFirstHalf.put(146, 60.37735849056604);
        cardToWeightFirstHalf.put(147, 43.624161073825505);
        cardToWeightFirstHalf.put(148, 53.84615384615385);
        cardToWeightFirstHalf.put(149, 38.297872340425535);
        cardToWeightFirstHalf.put(150, 0.0);
        cardToWeightFirstHalf.put(151, 35.0);
        cardToWeightFirstHalf.put(152, 0.0);
        cardToWeightFirstHalf.put(153, 54.621848739495796);
        cardToWeightFirstHalf.put(154, 0.0);
        cardToWeightFirstHalf.put(155, 0.0);
        cardToWeightFirstHalf.put(156, 65.60283687943263);
        cardToWeightFirstHalf.put(157, 63.63636363636363);
        cardToWeightFirstHalf.put(158, 61.30268199233716);
        cardToWeightFirstHalf.put(159, 78.125);
        cardToWeightFirstHalf.put(160, 52.980132450331126);
        cardToWeightFirstHalf.put(161, 50.0);
        cardToWeightFirstHalf.put(162, 0.0);
        cardToWeightFirstHalf.put(163, 50.0);
        cardToWeightFirstHalf.put(164, 0.0);
        cardToWeightFirstHalf.put(165, 64.56692913385827);
        cardToWeightFirstHalf.put(166, 0.0);
        cardToWeightFirstHalf.put(167, 0.0);
        cardToWeightFirstHalf.put(168, 0.0);
        cardToWeightFirstHalf.put(169, 0.0);
        cardToWeightFirstHalf.put(170, 37.5);
        cardToWeightFirstHalf.put(171, 0.0);
        cardToWeightFirstHalf.put(172, 0.0);
        cardToWeightFirstHalf.put(173, 100.0);
        cardToWeightFirstHalf.put(174, 60.32608695652174);
        cardToWeightFirstHalf.put(175, 45.19230769230769);
        cardToWeightFirstHalf.put(176, 30.76923076923077);
        cardToWeightFirstHalf.put(177, 0.0);
        cardToWeightFirstHalf.put(178, 76.14678899082568);
        cardToWeightFirstHalf.put(179, 43.39622641509434);
        cardToWeightFirstHalf.put(180, 56.75675675675676);
        cardToWeightFirstHalf.put(181, 63.73626373626374);
        cardToWeightFirstHalf.put(182, 0.0);
        cardToWeightFirstHalf.put(183, 48.611111111111114);
        cardToWeightFirstHalf.put(184, 0.0);
        cardToWeightFirstHalf.put(185, 32.8125);
        cardToWeightFirstHalf.put(186, 0.0);
        cardToWeightFirstHalf.put(187, 11.11111111111111);
        cardToWeightFirstHalf.put(188, 51.724137931034484);
        cardToWeightFirstHalf.put(189, 59.310344827586206);
        cardToWeightFirstHalf.put(190, 0.0);
        cardToWeightFirstHalf.put(191, 54.4973544973545);
        cardToWeightFirstHalf.put(192, 56.111111111111114);
        cardToWeightFirstHalf.put(193, 18.181818181818183);
        cardToWeightFirstHalf.put(194, 49.20634920634921);
        cardToWeightFirstHalf.put(195, 30.612244897959183);
        cardToWeightFirstHalf.put(196, 0.0);
        cardToWeightFirstHalf.put(197, 0.0);
        cardToWeightFirstHalf.put(198, 0.0);
        cardToWeightFirstHalf.put(199, 27.77777777777778);
        cardToWeightFirstHalf.put(200, 0.0);
        cardToWeightFirstHalf.put(201, 0.0);
        cardToWeightFirstHalf.put(202, 44.89795918367347);
        cardToWeightFirstHalf.put(203, 52.87958115183246);
        cardToWeightFirstHalf.put(204, 51.295336787564764);
        cardToWeightFirstHalf.put(205, 62.5);
        cardToWeightFirstHalf.put(206, 55.95238095238095);
        cardToWeightFirstHalf.put(207, 0.0);
        cardToWeightFirstHalf.put(208, 54.54545454545455);
        cardToWeightFirstHalf.put(209, 65.14522821576763);
        cardToWeightFirstHalf.put(210, 53.40909090909091);
        cardToWeightFirstHalf.put(211, 0.0);
        cardToWeightFirstHalf.put(212, 61.79775280898876);
        cardToWeightFirstHalf.put(213, 52.94117647058823);
        cardToWeightFirstHalf.put(214, 62.5);
        cardToWeightFirstHalf.put(215, 0.0);
        cardToWeightFirstHalf.put(216, 0.0);
        cardToWeightFirstHalf.put(217, 62.5);
        cardToWeightFirstHalf.put(218, 64.15094339622641);
        cardToWeightFirstHalf.put(219, 40.0);
        cardToWeightSecondHalf.put(1, 38.16425120772947);
        cardToWeightSecondHalf.put(2, 56.79012345679013);
        cardToWeightSecondHalf.put(3, 42.93478260869565);
        cardToWeightSecondHalf.put(4, 78.76344086021506);
        cardToWeightSecondHalf.put(5, 0.0);
        cardToWeightSecondHalf.put(6, 83.70927318295739);
        cardToWeightSecondHalf.put(7, 0.0);
        cardToWeightSecondHalf.put(8, 0.0);
        cardToWeightSecondHalf.put(9, 63.63636363636363);
        cardToWeightSecondHalf.put(10, 48.92086330935252);
        cardToWeightSecondHalf.put(11, 46.478873239436616);
        cardToWeightSecondHalf.put(12, 0.0);
        cardToWeightSecondHalf.put(13, 46.42857142857143);
        cardToWeightSecondHalf.put(14, 65.06849315068493);
        cardToWeightSecondHalf.put(15, 41.935483870967744);
        cardToWeightSecondHalf.put(16, 0.0);
        cardToWeightSecondHalf.put(17, 64.4776119402985);
        cardToWeightSecondHalf.put(18, 50.0);
        cardToWeightSecondHalf.put(19, 25.49019607843137);
        cardToWeightSecondHalf.put(20, 0.0);
        cardToWeightSecondHalf.put(21, 9.090909090909092);
        cardToWeightSecondHalf.put(22, 56.43564356435643);
        cardToWeightSecondHalf.put(23, 70.09646302250803);
        cardToWeightSecondHalf.put(24, 0.0);
        cardToWeightSecondHalf.put(25, 61.16838487972509);
        cardToWeightSecondHalf.put(26, 58.3732057416268);
        cardToWeightSecondHalf.put(27, 0.0);
        cardToWeightSecondHalf.put(28, 66.66666666666667);
        cardToWeightSecondHalf.put(29, 0.0);
        cardToWeightSecondHalf.put(30, 73.46938775510205);
        cardToWeightSecondHalf.put(31, 51.25);
        cardToWeightSecondHalf.put(32, 0.0);
        cardToWeightSecondHalf.put(33, 36.734693877551024);
        cardToWeightSecondHalf.put(34, 42.25352112676056);
        cardToWeightSecondHalf.put(35, 64.25339366515837);
        cardToWeightSecondHalf.put(36, 56.75675675675676);
        cardToWeightSecondHalf.put(37, 64.65517241379311);
        cardToWeightSecondHalf.put(38, 66.0968660968661);
        cardToWeightSecondHalf.put(39, 100.0);
        cardToWeightSecondHalf.put(40, 63.492063492063494);
        cardToWeightSecondHalf.put(41, 0.0);
        cardToWeightSecondHalf.put(42, 43.10344827586207);
        cardToWeightSecondHalf.put(43, 0.0);
        cardToWeightSecondHalf.put(44, 69.42857142857143);
        cardToWeightSecondHalf.put(45, 50.847457627118644);
        cardToWeightSecondHalf.put(46, 71.89189189189189);
        cardToWeightSecondHalf.put(47, 52.99145299145299);
        cardToWeightSecondHalf.put(48, 59.25925925925926);
        cardToWeightSecondHalf.put(49, 53.333333333333336);
        cardToWeightSecondHalf.put(50, 34.375);
        cardToWeightSecondHalf.put(51, 53.211009174311926);
        cardToWeightSecondHalf.put(52, 44.88636363636363);
        cardToWeightSecondHalf.put(53, 52.121212121212125);
        cardToWeightSecondHalf.put(54, 63.09148264984227);
        cardToWeightSecondHalf.put(55, 49.800796812749006);
        cardToWeightSecondHalf.put(56, 70.26315789473684);
        cardToWeightSecondHalf.put(57, 44.11764705882353);
        cardToWeightSecondHalf.put(58, 42.666666666666664);
        cardToWeightSecondHalf.put(59, 50.0);
        cardToWeightSecondHalf.put(60, 57.291666666666664);
        cardToWeightSecondHalf.put(61, 36.70886075949367);
        cardToWeightSecondHalf.put(62, 64.25855513307985);
        cardToWeightSecondHalf.put(63, 53.84615384615385);
        cardToWeightSecondHalf.put(64, 36.55913978494624);
        cardToWeightSecondHalf.put(65, 56.52173913043478);
        cardToWeightSecondHalf.put(66, 56.38766519823788);
        cardToWeightSecondHalf.put(67, 52.071005917159766);
        cardToWeightSecondHalf.put(68, 53.74149659863946);
        cardToWeightSecondHalf.put(69, 54.824561403508774);
        cardToWeightSecondHalf.put(70, 61.38996138996139);
        cardToWeightSecondHalf.put(71, 0.0);
        cardToWeightSecondHalf.put(72, 63.07692307692308);
        cardToWeightSecondHalf.put(73, 49.46808510638298);
        cardToWeightSecondHalf.put(74, 59.274193548387096);
        cardToWeightSecondHalf.put(75, 43.61233480176212);
        cardToWeightSecondHalf.put(76, 64.2023346303502);
        cardToWeightSecondHalf.put(77, 67.61565836298932);
        cardToWeightSecondHalf.put(78, 64.06926406926407);
        cardToWeightSecondHalf.put(79, 47.82608695652174);
        cardToWeightSecondHalf.put(80, 61.31386861313869);
        cardToWeightSecondHalf.put(81, 73.2484076433121);
        cardToWeightSecondHalf.put(82, 79.70660146699267);
        cardToWeightSecondHalf.put(83, 62.23175965665236);
        cardToWeightSecondHalf.put(84, 68.91025641025641);
        cardToWeightSecondHalf.put(85, 66.2125340599455);
        cardToWeightSecondHalf.put(86, 63.18181818181818);
        cardToWeightSecondHalf.put(87, 71.42857142857143);
        cardToWeightSecondHalf.put(88, 44.96124031007752);
        cardToWeightSecondHalf.put(89, 59.375);
        cardToWeightSecondHalf.put(90, 48.03921568627451);
        cardToWeightSecondHalf.put(91, 42.57425742574257);
        cardToWeightSecondHalf.put(92, 47.972972972972975);
        cardToWeightSecondHalf.put(93, 64.50704225352112);
        cardToWeightSecondHalf.put(94, 50.42016806722689);
        cardToWeightSecondHalf.put(95, 57.00934579439252);
        cardToWeightSecondHalf.put(96, 66.90140845070422);
        cardToWeightSecondHalf.put(97, 44.85981308411215);
        cardToWeightSecondHalf.put(98, 53.43511450381679);
        cardToWeightSecondHalf.put(99, 69.42446043165468);
        cardToWeightSecondHalf.put(100, 70.21857923497268);
        cardToWeightSecondHalf.put(101, 66.996699669967);
        cardToWeightSecondHalf.put(102, 48.31460674157304);
        cardToWeightSecondHalf.put(103, 57.89473684210526);
        cardToWeightSecondHalf.put(104, 18.181818181818183);
        cardToWeightSecondHalf.put(105, 0.0);
        cardToWeightSecondHalf.put(106, 0.0);
        cardToWeightSecondHalf.put(107, 29.292929292929294);
        cardToWeightSecondHalf.put(108, 33.333333333333336);
        cardToWeightSecondHalf.put(109, 50.0);
        cardToWeightSecondHalf.put(110, 66.44295302013423);
        cardToWeightSecondHalf.put(111, 0.0);
        cardToWeightSecondHalf.put(112, 41.37931034482759);
        cardToWeightSecondHalf.put(113, 68.1159420289855);
        cardToWeightSecondHalf.put(114, 58.8235294117647);
        cardToWeightSecondHalf.put(115, 25.806451612903224);
        cardToWeightSecondHalf.put(116, 70.3601108033241);
        cardToWeightSecondHalf.put(117, 30.555555555555557);
        cardToWeightSecondHalf.put(118, 0.0);
        cardToWeightSecondHalf.put(119, 48.717948717948715);
        cardToWeightSecondHalf.put(120, 37.735849056603776);
        cardToWeightSecondHalf.put(121, 0.0);
        cardToWeightSecondHalf.put(122, 68.5121107266436);
        cardToWeightSecondHalf.put(123, 67.6056338028169);
        cardToWeightSecondHalf.put(124, 47.36842105263158);
        cardToWeightSecondHalf.put(125, 41.07142857142857);
        cardToWeightSecondHalf.put(126, 50.0);
        cardToWeightSecondHalf.put(127, 0.0);
        cardToWeightSecondHalf.put(128, 0.0);
        cardToWeightSecondHalf.put(129, 56.06060606060606);
        cardToWeightSecondHalf.put(130, 28.676470588235293);
        cardToWeightSecondHalf.put(131, 47.972972972972975);
        cardToWeightSecondHalf.put(132, 34.22818791946309);
        cardToWeightSecondHalf.put(133, 34.285714285714285);
        cardToWeightSecondHalf.put(134, 0.0);
        cardToWeightSecondHalf.put(135, 21.73913043478261);
        cardToWeightSecondHalf.put(136, 62.171052631578945);
        cardToWeightSecondHalf.put(137, 60.57692307692308);
        cardToWeightSecondHalf.put(138, 60.19900497512438);
        cardToWeightSecondHalf.put(139, 48.57142857142857);
        cardToWeightSecondHalf.put(140, 47.36842105263158);
        cardToWeightSecondHalf.put(141, 64.28571428571429);
        cardToWeightSecondHalf.put(142, 0.0);
        cardToWeightSecondHalf.put(143, 39.285714285714285);
        cardToWeightSecondHalf.put(144, 0.0);
        cardToWeightSecondHalf.put(145, 0.0);
        cardToWeightSecondHalf.put(146, 57.59162303664922);
        cardToWeightSecondHalf.put(147, 48.76543209876543);
        cardToWeightSecondHalf.put(148, 46.25);
        cardToWeightSecondHalf.put(149, 29.62962962962963);
        cardToWeightSecondHalf.put(150, 60.0);
        cardToWeightSecondHalf.put(151, 24.137931034482758);
        cardToWeightSecondHalf.put(152, 0.0);
        cardToWeightSecondHalf.put(153, 58.50340136054422);
        cardToWeightSecondHalf.put(154, 0.0);
        cardToWeightSecondHalf.put(155, 0.0);
        cardToWeightSecondHalf.put(156, 72.33502538071066);
        cardToWeightSecondHalf.put(157, 57.7720207253886);
        cardToWeightSecondHalf.put(158, 53.96825396825397);
        cardToWeightSecondHalf.put(159, 74.85875706214689);
        cardToWeightSecondHalf.put(160, 64.26592797783934);
        cardToWeightSecondHalf.put(161, 49.78540772532189);
        cardToWeightSecondHalf.put(162, 0.0);
        cardToWeightSecondHalf.put(163, 37.79904306220096);
        cardToWeightSecondHalf.put(164, 0.0);
        cardToWeightSecondHalf.put(165, 70.52631578947368);
        cardToWeightSecondHalf.put(166, 0.0);
        cardToWeightSecondHalf.put(167, 0.0);
        cardToWeightSecondHalf.put(168, 0.0);
        cardToWeightSecondHalf.put(169, 20.833333333333332);
        cardToWeightSecondHalf.put(170, 29.166666666666668);
        cardToWeightSecondHalf.put(171, 0.0);
        cardToWeightSecondHalf.put(172, 17.1875);
        cardToWeightSecondHalf.put(173, 14.285714285714286);
        cardToWeightSecondHalf.put(174, 57.635467980295566);
        cardToWeightSecondHalf.put(175, 40.60606060606061);
        cardToWeightSecondHalf.put(176, 45.945945945945944);
        cardToWeightSecondHalf.put(177, 0.0);
        cardToWeightSecondHalf.put(178, 69.96805111821087);
        cardToWeightSecondHalf.put(179, 20.0);
        cardToWeightSecondHalf.put(180, 72.0);
        cardToWeightSecondHalf.put(181, 71.66212534059946);
        cardToWeightSecondHalf.put(182, 0.0);
        cardToWeightSecondHalf.put(183, 29.032258064516128);
        cardToWeightSecondHalf.put(184, 0.0);
        cardToWeightSecondHalf.put(185, 50.0);
        cardToWeightSecondHalf.put(186, 0.0);
        cardToWeightSecondHalf.put(187, 49.35064935064935);
        cardToWeightSecondHalf.put(188, 53.763440860215056);
        cardToWeightSecondHalf.put(189, 65.11627906976744);
        cardToWeightSecondHalf.put(190, 0.0);
        cardToWeightSecondHalf.put(191, 54.54545454545455);
        cardToWeightSecondHalf.put(192, 41.0958904109589);
        cardToWeightSecondHalf.put(193, 44.89795918367347);
        cardToWeightSecondHalf.put(194, 36.8421052631579);
        cardToWeightSecondHalf.put(195, 33.333333333333336);
        cardToWeightSecondHalf.put(196, 0.0);
        cardToWeightSecondHalf.put(197, 10.0);
        cardToWeightSecondHalf.put(198, 0.0);
        cardToWeightSecondHalf.put(199, 46.927374301675975);
        cardToWeightSecondHalf.put(200, 0.0);
        cardToWeightSecondHalf.put(201, 0.0);
        cardToWeightSecondHalf.put(202, 41.37931034482759);
        cardToWeightSecondHalf.put(203, 61.347517730496456);
        cardToWeightSecondHalf.put(204, 62.1160409556314);
        cardToWeightSecondHalf.put(205, 32.758620689655174);
        cardToWeightSecondHalf.put(206, 65.83850931677019);
        cardToWeightSecondHalf.put(207, 0.0);
        cardToWeightSecondHalf.put(208, 44.1025641025641);
        cardToWeightSecondHalf.put(209, 50.869565217391305);
        cardToWeightSecondHalf.put(210, 40.0);
        cardToWeightSecondHalf.put(211, 0.0);
        cardToWeightSecondHalf.put(212, 65.66757493188011);
        cardToWeightSecondHalf.put(213, 50.3875968992248);
        cardToWeightSecondHalf.put(214, 68.02507836990596);
        cardToWeightSecondHalf.put(215, 0.0);
        cardToWeightSecondHalf.put(216, 0.0);
        cardToWeightSecondHalf.put(217, 55.714285714285715);
        cardToWeightSecondHalf.put(218, 53.6231884057971);
        cardToWeightSecondHalf.put(219, 36.36363636363637);
    }

    public static final double MIDDLE_TURN = 19.245;

    public Integer getWorstCard(MarsGame game, Player player, List<Integer> cards, int turn) {
        double firstHalfCoefficient;
        double secondHalfCoefficient;
        if (turn < MIDDLE_TURN) {
            firstHalfCoefficient = (double) turn / MIDDLE_TURN;
            secondHalfCoefficient = 1.0 - firstHalfCoefficient;
        } else {
            firstHalfCoefficient = 0.0;
            secondHalfCoefficient = 1.0;
        }

        double worstCard = 500.0;
        int worstCardIndex = 0;

        for (int i = 0; i < cards.size(); i++) {
            double worth = getCardWorth(cards.get(i), firstHalfCoefficient, secondHalfCoefficient, game, player, turn);
            if (worth < worstCard) {
                worstCard = worth;
                worstCardIndex = i;
            }
        }


        return cards.get(worstCardIndex);
    }

    public Card getBestCardAsCard(MarsGame game, Player player, List<Card> cards, int turn, boolean ignoreCardIfBad) {
        if (cards.isEmpty()) {
            return null;
        }

        double firstHalfCoefficient;
        double secondHalfCoefficient;
        if (turn < MIDDLE_TURN) {
            firstHalfCoefficient = (double) turn / MIDDLE_TURN;
            secondHalfCoefficient = 1.0 - firstHalfCoefficient;
        } else {
            firstHalfCoefficient = 0.0;
            secondHalfCoefficient = 1.0;
        }

        double bestCardWorth = 0.0;
        Card bestCard = cards.get(0);

        for (int i = 0; i < cards.size(); i++) {
            double worth = getCardWorth(cards.get(i).getId(), firstHalfCoefficient, secondHalfCoefficient, game, player, turn);
            if (worth >= bestCardWorth) {
                bestCardWorth = worth;
                bestCard = cards.get(i);
            }
        }

        if (ignoreCardIfBad && bestCardWorth <= 50.0) {
            return null;
        }

        return bestCard;
    }

    public Integer getBestCard(MarsGame game, Player player, List<Integer> cards, int turn) {
        double firstHalfCoefficient;
        double secondHalfCoefficient;
        if (turn < MIDDLE_TURN) {
            firstHalfCoefficient = (double) turn / MIDDLE_TURN;
            secondHalfCoefficient = 1.0 - firstHalfCoefficient;
        } else {
            firstHalfCoefficient = 0.0;
            secondHalfCoefficient = 1.0;
        }

        double bestCard = 0.0;
        int bestCardId = 0;

        for (int i = 0; i < cards.size(); i++) {
            double worth = getCardWorth(cards.get(i), firstHalfCoefficient, secondHalfCoefficient, game, player, turn);
            if (worth >= bestCard) {
                bestCard = worth;
                bestCardId = cards.get(i);
            }
        }

        return bestCardId;
    }

    private double getCardCoefficient(MarsGame game, Player player, Card card, int turn) {

        double coefficient = 1.0;
        if (card.getSpecialEffects().contains(SpecialEffect.ADVANCED_ALLOYS)) {
            if (turn < MIDDLE_TURN * 1.75 && (player.getSteelIncome() > 0 || player.getTitaniumIncome() > 1)) {
                coefficient = 1.3;
            }
        } else if (card.getSpecialEffects().contains(SpecialEffect.SOLD_CARDS_COST_1_MC_MORE)) {
            coefficient = 1.3;
        } else if (card.getSpecialEffects().contains(SpecialEffect.ENERGY_SUBSIDIES_DISCOUNT_4)
                || card.getSpecialEffects().contains(SpecialEffect.INTERPLANETARY_CONFERENCE)
                || card.getSpecialEffects().contains(SpecialEffect.MEDIA_GROUP)) {
            if (turn < MIDDLE_TURN * 1.25) {
                coefficient = 1.3;
            }
        } else if (card.getSpecialEffects().contains(SpecialEffect.EXTENDED_RESOURCES)
                || card.getSpecialEffects().contains(SpecialEffect.INTERNS)
                || card.getSpecialEffects().contains(SpecialEffect.UNITED_PLANETARY_ALLIANCE)) {
            if (turn < MIDDLE_TURN) {
                coefficient = 1.5;
            }
        } else {
            coefficient = Optional.ofNullable(card.getCardMetadata()).map(CardMetadata::getCardAction).map(
                    cardAction -> {
                        if (cardAction == CardAction.RECYCLED_DETRITUS && turn < MIDDLE_TURN * 1.25) {
                            return 1.5;
                        }
                        if (cardAction == CardAction.RESTRUCTURED_RESOURCES && turn < MIDDLE_TURN * 1.25) {
                            return 1.5;
                        }
                        if ((cardAction == CardAction.ANTI_GRAVITY_TECH || cardAction == CardAction.AI_CENTRAL) && turn < MIDDLE_TURN * 1.75) {
                            if (cardService.countPlayedTags(player, Set.of(Tag.SCIENCE)) >= 5) {
                                return MAX_PRIORITY;
                            } else {
                                return 1.5;
                            }
                        }
                        if (cardAction == CardAction.OLYMPUS_CONFERENCE || cardAction == CardAction.MARS_UNIVERSITY) {
                            return 1.3;
                        }
                        if (cardAction == CardAction.AQUIFER_PUMPING && player.getSteelIncome() < 3) {
                            return 0.0;
                        }
                        if ((cardAction == CardAction.AQUIFER_PUMPING
                                || cardAction == CardAction.ARCTIC_ALGAE
                                || cardAction == CardAction.FISH
                                || cardAction == CardAction.NITRITE_REDUCTING
                                || cardAction == CardAction.VOLCANIC_POOLS)
                                && game.getPlanet().isOceansMax()) {
                            return 0.0;
                        }
                        if ((cardAction == CardAction.DEVELOPED_INFRASTRUCTURE
                                || cardAction == CardAction.GHG_PRODUCTION
                                || cardAction == CardAction.LIVESTOCK
                                || cardAction == CardAction.WOOD_BURNING_STOVES) && game.getPlanet().isTemperatureMax()) {
                            return 0.0;
                        }
                        if ((cardAction == CardAction.FARMING_COOPS
                                || cardAction == CardAction.GREEN_HOUSES
                                || cardAction == CardAction.IRON_WORKS
                                || cardAction == CardAction.REGOLITH_EATERS
                                || cardAction == CardAction.STEELWORKS
                                || cardAction == CardAction.PROGRESSIVE_POLICIES) && game.getPlanet().isOxygenMax()) {
                            return 0.0;
                        }

                        if (cardAction == CardAction.HEAT_EARTH_INCOME) {
                            int playedTags = cardService.countPlayedTags(player, Set.of(Tag.EARTH));
                            if (playedTags > 0) {
                                return 1.0 + playedTags * 0.05;
                            }
                        }

                        if (cardAction == CardAction.MC_ANIMAL_PLANT_INCOME) {
                            int playedTags = cardService.countPlayedTags(player, Set.of(Tag.ANIMAL, Tag.PLANT));
                            if (playedTags > 0) {
                                return 1.0 + playedTags * 0.05;
                            }
                        }

                        if (cardAction == CardAction.MC_ANIMAL_PLANT_INCOME) {
                            int playedTags = cardService.countPlayedTags(player, Set.of(Tag.ANIMAL, Tag.PLANT));
                            if (playedTags > 0) {
                                return 1.0 + playedTags * 0.05;
                            }
                        }

                        if (cardAction == CardAction.CARD_SCIENCE_INCOME) {
                            int playedTags = cardService.countPlayedTags(player, Set.of(Tag.SCIENCE));
                            if (playedTags > 0) {
                                return 1.0 + playedTags * 0.05;
                            }
                        }

                        if (cardAction == CardAction.MC_EARTH_INCOME) {
                            int playedTags = cardService.countPlayedTags(player, Set.of(Tag.EARTH));
                            if (playedTags > 0) {
                                return 1.0 + playedTags * 0.05;
                            }
                        }

                        if (cardAction == CardAction.PLANT_PLANT_INCOME) {
                            int playedTags = cardService.countPlayedTags(player, Set.of(Tag.PLANT));
                            if (playedTags > 0) {
                                return 1.0 + playedTags * 0.05;
                            }
                        }

                        if (cardAction == CardAction.MC_SCIENCE_INCOME) {
                            int playedTags = cardService.countPlayedTags(player, Set.of(Tag.SCIENCE));
                            if (playedTags > 0) {
                                return 1.0 + playedTags * 0.05;
                            }
                        }

                        if (cardAction == CardAction.MC_2_BUILDING_INCOME) {
                            int playedTags = cardService.countPlayedTags(player, Set.of(Tag.BUILDING));
                            if (playedTags > 1) {
                                return 1.0 + playedTags * 0.025;
                            }
                        }

                        if (cardAction == CardAction.MC_ENERGY_INCOME) {
                            int playedTags = cardService.countPlayedTags(player, Set.of(Tag.ENERGY));
                            if (playedTags > 0) {
                                return 1.0 + playedTags * 0.05;
                            }
                        }

                        if (cardAction == CardAction.MC_SPACE_INCOME) {
                            int playedTags = cardService.countPlayedTags(player, Set.of(Tag.SPACE));
                            if (playedTags > 0) {
                                return 1.0 + playedTags * 0.05;
                            }
                        }

                        if (cardAction == CardAction.HEAT_SPACE_INCOME) {
                            int playedTags = cardService.countPlayedTags(player, Set.of(Tag.SPACE));
                            if (playedTags > 0) {
                                return 1.0 + playedTags * 0.05;
                            }
                        }

                        if (cardAction == CardAction.MC_EVENT_INCOME) {
                            int playedTags = cardService.countPlayedTags(player, Set.of(Tag.EVENT));
                            if (playedTags > 0) {
                                return 1.0 + playedTags * 0.05;
                            }
                        }

                        if (cardAction == CardAction.HEAT_ENERGY_INCOME) {
                            int playedTags = cardService.countPlayedTags(player, Set.of(Tag.ENERGY));
                            if (playedTags > 0) {
                                return 1.0 + playedTags * 0.05;
                            }
                        }

                        if (cardAction == CardAction.PLANT_MICROBE_INCOME) {
                            int playedTags = cardService.countPlayedTags(player, Set.of(Tag.MICROBE));
                            if (playedTags > 0) {
                                return 1.0 + playedTags * 0.05;
                            }
                        }

                        if (cardAction == CardAction.MC_FOREST_INCOME) {
                            if (player.getForests() > 0) {
                                return 1.0 + player.getForests() * 0.05;
                            }
                        }

                        if (cardAction == CardAction.STEELWORKS || cardAction == CardAction.IRON_WORKS) {
                            if (player.getHeatIncome() > 5) {
                                return 1.5;
                            } else {
                                return 1.25;
                            }
                        }

                        if (cardAction == CardAction.CARETAKER_CONTRACT) {
                            if (player.getHeatIncome() >= 8) {
                                return 1.5;
                            } else if (player.getHeatIncome() >= 4) {
                                return 1.25;
                            }
                        }

                        if (cardAction == CardAction.POWER_INFRASTRUCTURE && game.getPlanet().isTemperatureMax()) {
                            return MAX_PRIORITY;
                        }

                        return null;
                    }
            ).orElse(1.0);
        }

        int cardDiscount = paymentValidationService.getDiscount(card, player);

        if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.ENERGY_SUBSIDIES_DISCOUNT_4)) {
            cardDiscount += countCardTags(card, Tag.ENERGY) * 4;
        }

        if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.INTERPLANETARY_CONFERENCE)) {
            cardDiscount += countCardTags(card, Tag.EARTH) * 4;
            cardDiscount += countCardTags(card, Tag.JUPITER) * 4;
        }

        if (card.getTags().contains(Tag.EVENT) && hasCardAction(player, CardAction.OPTIMAL_AEROBRAKING)) {
            cardDiscount += 8;
        }

        if (card.getTags().contains(Tag.EVENT) && hasCardAction(player, CardAction.RECYCLED_DETRITUS)) {
            cardDiscount += 8;
        }

        double ratio = ((double) cardDiscount / card.getPrice());

        double discountCoefficient = 1.00;
        if (ratio > 0 && ratio < 0.25) {
            discountCoefficient = 1.05;
        } else if (ratio >= 0.25 && ratio < 0.5) {
            discountCoefficient = 1.15;
        } else if (ratio >= 0.5 && ratio < 0.75) {
            discountCoefficient = 1.25;
        } else if (ratio >= 0.75) {
            discountCoefficient = 1.35;
        }

        if (turn >= MIDDLE_TURN * 1.25 && card.getWinningPoints() > 0) {
            discountCoefficient *= 1.25;
        }

        coefficient *= discountCoefficient;

        return coefficient;
    }

    private boolean hasCardAction(Player player, CardAction cardAction) {
        return player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .map(Card::getCardMetadata)
                .filter(Objects::nonNull)
                .map(CardMetadata::getCardAction)
                .filter(Objects::nonNull)
                .anyMatch(action -> action == cardAction);
    }

    private int countCardTags(Card card, Tag tag) {
        return (int) card.getTags().stream().filter(t -> t == tag).count();
    }

    private double getCardWorth(Integer card, double firstHalfCoefficient, double secondHalfCoefficient, MarsGame game, Player player, int turn) {
        return cardToWeightFirstHalf.get(card) * firstHalfCoefficient + cardToWeightSecondHalf.get(card) * secondHalfCoefficient
                * getCardCoefficient(game, player, cardService.getCard(card), turn);
    }

}
