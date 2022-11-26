package com.terraforming.ares.services.ai;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
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
        cardToWeightFirstHalf.put(1, 34.61538461538461);
        cardToWeightFirstHalf.put(2, 50.81967213114754);
        cardToWeightFirstHalf.put(3, 52.121212121212125);
        cardToWeightFirstHalf.put(4, 75.0);
        cardToWeightFirstHalf.put(5, 50.370370370370374);
        cardToWeightFirstHalf.put(6, 82.08955223880596);
        cardToWeightFirstHalf.put(7, 40.0);
        cardToWeightFirstHalf.put(8, 33.333333333333336);
        cardToWeightFirstHalf.put(9, 43.82022471910113);
        cardToWeightFirstHalf.put(10, 45.0);
        cardToWeightFirstHalf.put(11, 50.96774193548387);
        cardToWeightFirstHalf.put(12, 0.0);
        cardToWeightFirstHalf.put(13, 41.95402298850575);
        cardToWeightFirstHalf.put(14, 0.0);
        cardToWeightFirstHalf.put(15, 44.705882352941174);
        cardToWeightFirstHalf.put(16, 38.46153846153846);
        cardToWeightFirstHalf.put(17, 49.554896142433236);
        cardToWeightFirstHalf.put(18, 30.0);
        cardToWeightFirstHalf.put(19, 36.8421052631579);
        cardToWeightFirstHalf.put(20, 31.03448275862069);
        cardToWeightFirstHalf.put(21, 54.23728813559322);
        cardToWeightFirstHalf.put(22, 46.69260700389105);
        cardToWeightFirstHalf.put(23, 53.30739299610895);
        cardToWeightFirstHalf.put(24, 47.5609756097561);
        cardToWeightFirstHalf.put(25, 59.1324200913242);
        cardToWeightFirstHalf.put(26, 62.15538847117794);
        cardToWeightFirstHalf.put(27, 34.523809523809526);
        cardToWeightFirstHalf.put(28, 41.75824175824176);
        cardToWeightFirstHalf.put(29, 35.0);
        cardToWeightFirstHalf.put(30, 47.05882352941177);
        cardToWeightFirstHalf.put(31, 57.142857142857146);
        cardToWeightFirstHalf.put(32, 33.333333333333336);
        cardToWeightFirstHalf.put(33, 70.58823529411765);
        cardToWeightFirstHalf.put(34, 42.2680412371134);
        cardToWeightFirstHalf.put(35, 69.11764705882354);
        cardToWeightFirstHalf.put(36, 59.682539682539684);
        cardToWeightFirstHalf.put(37, 63.9344262295082);
        cardToWeightFirstHalf.put(38, 39.053254437869825);
        cardToWeightFirstHalf.put(39, 100.0);
        cardToWeightFirstHalf.put(40, 49.06542056074766);
        cardToWeightFirstHalf.put(41, 45.270270270270274);
        cardToWeightFirstHalf.put(42, 53.645833333333336);
        cardToWeightFirstHalf.put(43, 37.735849056603776);
        cardToWeightFirstHalf.put(44, 60.150375939849624);
        cardToWeightFirstHalf.put(45, 55.81395348837209);
        cardToWeightFirstHalf.put(46, 72.12121212121212);
        cardToWeightFirstHalf.put(47, 50.46296296296296);
        cardToWeightFirstHalf.put(48, 61.30653266331658);
        cardToWeightFirstHalf.put(49, 29.032258064516128);
        cardToWeightFirstHalf.put(50, 33.333333333333336);
        cardToWeightFirstHalf.put(51, 52.23880597014925);
        cardToWeightFirstHalf.put(52, 14.285714285714286);
        cardToWeightFirstHalf.put(53, 61.19733924611973);
        cardToWeightFirstHalf.put(54, 44.31372549019608);
        cardToWeightFirstHalf.put(55, 29.310344827586206);
        cardToWeightFirstHalf.put(56, 39.53488372093023);
        cardToWeightFirstHalf.put(57, 45.45454545454545);
        cardToWeightFirstHalf.put(58, 49.142857142857146);
        cardToWeightFirstHalf.put(59, 53.57142857142857);
        cardToWeightFirstHalf.put(60, 65.4292343387471);
        cardToWeightFirstHalf.put(61, 47.05882352941177);
        cardToWeightFirstHalf.put(62, 46.31578947368421);
        cardToWeightFirstHalf.put(63, 44.067796610169495);
        cardToWeightFirstHalf.put(64, 47.096774193548384);
        cardToWeightFirstHalf.put(65, 16.666666666666668);
        cardToWeightFirstHalf.put(66, 0.0);
        cardToWeightFirstHalf.put(67, 57.02479338842975);
        cardToWeightFirstHalf.put(68, 50.0);
        cardToWeightFirstHalf.put(69, 55.66502463054187);
        cardToWeightFirstHalf.put(70, 59.20245398773006);
        cardToWeightFirstHalf.put(71, 51.19047619047619);
        cardToWeightFirstHalf.put(72, 50.20746887966805);
        cardToWeightFirstHalf.put(73, 53.46534653465346);
        cardToWeightFirstHalf.put(74, 54.76190476190476);
        cardToWeightFirstHalf.put(75, 54.08560311284047);
        cardToWeightFirstHalf.put(76, 55.033557046979865);
        cardToWeightFirstHalf.put(77, 60.19417475728155);
        cardToWeightFirstHalf.put(78, 61.94331983805668);
        cardToWeightFirstHalf.put(79, 0.0);
        cardToWeightFirstHalf.put(80, 54.57142857142857);
        cardToWeightFirstHalf.put(81, 54.54545454545455);
        cardToWeightFirstHalf.put(82, 60.869565217391305);
        cardToWeightFirstHalf.put(83, 57.89473684210526);
        cardToWeightFirstHalf.put(84, 54.02597402597402);
        cardToWeightFirstHalf.put(85, 53.793103448275865);
        cardToWeightFirstHalf.put(86, 0.0);
        cardToWeightFirstHalf.put(87, 59.375);
        cardToWeightFirstHalf.put(88, 53.44827586206897);
        cardToWeightFirstHalf.put(89, 51.698113207547166);
        cardToWeightFirstHalf.put(90, 0.0);
        cardToWeightFirstHalf.put(91, 47.88732394366197);
        cardToWeightFirstHalf.put(92, 0.0);
        cardToWeightFirstHalf.put(93, 47.674418604651166);
        cardToWeightFirstHalf.put(94, 80.3921568627451);
        cardToWeightFirstHalf.put(95, 51.875);
        cardToWeightFirstHalf.put(96, 54.41988950276243);
        cardToWeightFirstHalf.put(97, 45.744680851063826);
        cardToWeightFirstHalf.put(98, 47.20496894409938);
        cardToWeightFirstHalf.put(99, 58.843537414965986);
        cardToWeightFirstHalf.put(100, 48.598130841121495);
        cardToWeightFirstHalf.put(101, 52.98804780876494);
        cardToWeightFirstHalf.put(102, 51.690821256038646);
        cardToWeightFirstHalf.put(103, 51.65562913907285);
        cardToWeightFirstHalf.put(104, 54.0);
        cardToWeightFirstHalf.put(105, 0.0);
        cardToWeightFirstHalf.put(106, 33.333333333333336);
        cardToWeightFirstHalf.put(107, 0.0);
        cardToWeightFirstHalf.put(108, 50.0);
        cardToWeightFirstHalf.put(109, 28.94736842105263);
        cardToWeightFirstHalf.put(110, 63.46153846153846);
        cardToWeightFirstHalf.put(111, 46.666666666666664);
        cardToWeightFirstHalf.put(112, 50.87719298245614);
        cardToWeightFirstHalf.put(113, 49.845201238390096);
        cardToWeightFirstHalf.put(114, 33.333333333333336);
        cardToWeightFirstHalf.put(115, 44.51219512195122);
        cardToWeightFirstHalf.put(116, 53.5);
        cardToWeightFirstHalf.put(117, 53.03030303030303);
        cardToWeightFirstHalf.put(118, 47.27272727272727);
        cardToWeightFirstHalf.put(119, 56.09756097560975);
        cardToWeightFirstHalf.put(120, 53.92156862745098);
        cardToWeightFirstHalf.put(121, 57.142857142857146);
        cardToWeightFirstHalf.put(122, 54.889589905362776);
        cardToWeightFirstHalf.put(123, 54.6583850931677);
        cardToWeightFirstHalf.put(124, 50.70422535211268);
        cardToWeightFirstHalf.put(125, 86.20689655172414);
        cardToWeightFirstHalf.put(126, 38.80597014925373);
        cardToWeightFirstHalf.put(127, 46.42857142857143);
        cardToWeightFirstHalf.put(128, 42.857142857142854);
        cardToWeightFirstHalf.put(129, 45.833333333333336);
        cardToWeightFirstHalf.put(130, 48.167539267015705);
        cardToWeightFirstHalf.put(131, 61.53846153846154);
        cardToWeightFirstHalf.put(132, 20.0);
        cardToWeightFirstHalf.put(133, 0.0);
        cardToWeightFirstHalf.put(134, 50.79365079365079);
        cardToWeightFirstHalf.put(135, 44.44444444444444);
        cardToWeightFirstHalf.put(136, 43.558282208588956);
        cardToWeightFirstHalf.put(137, 62.62626262626262);
        cardToWeightFirstHalf.put(138, 57.06214689265537);
        cardToWeightFirstHalf.put(139, 50.0);
        cardToWeightFirstHalf.put(140, 51.25);
        cardToWeightFirstHalf.put(141, 46.28099173553719);
        cardToWeightFirstHalf.put(142, 33.333333333333336);
        cardToWeightFirstHalf.put(143, 48.10126582278481);
        cardToWeightFirstHalf.put(144, 51.61290322580645);
        cardToWeightFirstHalf.put(145, 50.0);
        cardToWeightFirstHalf.put(146, 49.681528662420384);
        cardToWeightFirstHalf.put(147, 47.44525547445255);
        cardToWeightFirstHalf.put(148, 49.46236559139785);
        cardToWeightFirstHalf.put(149, 56.0);
        cardToWeightFirstHalf.put(150, 41.1764705882353);
        cardToWeightFirstHalf.put(151, 50.64935064935065);
        cardToWeightFirstHalf.put(152, 46.666666666666664);
        cardToWeightFirstHalf.put(153, 65.38461538461539);
        cardToWeightFirstHalf.put(154, 75.0);
        cardToWeightFirstHalf.put(155, 46.91358024691358);
        cardToWeightFirstHalf.put(156, 61.05263157894737);
        cardToWeightFirstHalf.put(157, 100.0);
        cardToWeightFirstHalf.put(158, 47.12643678160919);
        cardToWeightFirstHalf.put(159, 69.06474820143885);
        cardToWeightFirstHalf.put(160, 42.42424242424242);
        cardToWeightFirstHalf.put(161, 28.571428571428573);
        cardToWeightFirstHalf.put(162, 50.90909090909091);
        cardToWeightFirstHalf.put(163, 43.84615384615385);
        cardToWeightFirstHalf.put(164, 47.567567567567565);
        cardToWeightFirstHalf.put(165, 55.51470588235294);
        cardToWeightFirstHalf.put(166, 31.25);
        cardToWeightFirstHalf.put(167, 34.375);
        cardToWeightFirstHalf.put(168, 28.571428571428573);
        cardToWeightFirstHalf.put(169, 0.0);
        cardToWeightFirstHalf.put(170, 57.69230769230769);
        cardToWeightFirstHalf.put(171, 36.36363636363637);
        cardToWeightFirstHalf.put(172, 23.529411764705884);
        cardToWeightFirstHalf.put(173, 39.097744360902254);
        cardToWeightFirstHalf.put(174, 51.1520737327189);
        cardToWeightFirstHalf.put(175, 43.24324324324324);
        cardToWeightFirstHalf.put(176, 48.07692307692308);
        cardToWeightFirstHalf.put(177, 20.0);
        cardToWeightFirstHalf.put(178, 64.24870466321244);
        cardToWeightFirstHalf.put(179, 45.45454545454545);
        cardToWeightFirstHalf.put(180, 49.13793103448276);
        cardToWeightFirstHalf.put(181, 52.11267605633803);
        cardToWeightFirstHalf.put(182, 50.76335877862596);
        cardToWeightFirstHalf.put(183, 53.191489361702125);
        cardToWeightFirstHalf.put(184, 35.0);
        cardToWeightFirstHalf.put(185, 45.91836734693877);
        cardToWeightFirstHalf.put(186, 44.18604651162791);
        cardToWeightFirstHalf.put(187, 41.37931034482759);
        cardToWeightFirstHalf.put(188, 50.0);
        cardToWeightFirstHalf.put(189, 43.262411347517734);
        cardToWeightFirstHalf.put(190, 41.53846153846154);
        cardToWeightFirstHalf.put(191, 44.0);
        cardToWeightFirstHalf.put(192, 51.16279069767442);
        cardToWeightFirstHalf.put(193, 53.50877192982456);
        cardToWeightFirstHalf.put(194, 53.46534653465346);
        cardToWeightFirstHalf.put(195, 45.45454545454545);
        cardToWeightFirstHalf.put(196, 58.333333333333336);
        cardToWeightFirstHalf.put(197, 72.0);
        cardToWeightFirstHalf.put(198, 0.0);
        cardToWeightFirstHalf.put(199, 0.0);
        cardToWeightFirstHalf.put(200, 0.0);
        cardToWeightFirstHalf.put(201, 46.3768115942029);
        cardToWeightFirstHalf.put(202, 55.37190082644628);
        cardToWeightFirstHalf.put(203, 42.857142857142854);
        cardToWeightFirstHalf.put(204, 51.02040816326531);
        cardToWeightFirstHalf.put(205, 55.55555555555556);
        cardToWeightFirstHalf.put(206, 52.78688524590164);
        cardToWeightFirstHalf.put(207, 0.0);
        cardToWeightFirstHalf.put(208, 100.0);
        cardToWeightFirstHalf.put(209, 48.89867841409691);
        cardToWeightFirstHalf.put(210, 56.70103092783505);
        cardToWeightFirstHalf.put(211, 46.2962962962963);
        cardToWeightFirstHalf.put(212, 58.97435897435897);
        cardToWeightFirstHalf.put(213, 49.30875576036866);
        cardToWeightFirstHalf.put(214, 61.67664670658683);
        cardToWeightFirstHalf.put(215, 50.0);
        cardToWeightFirstHalf.put(216, 42.622950819672134);
        cardToWeightFirstHalf.put(217, 50.0);
        cardToWeightFirstHalf.put(218, 52.534562211981566);
        cardToWeightFirstHalf.put(219, 42.857142857142854);
        cardToWeightSecondHalf.put(1, 38.70967741935484);
        cardToWeightSecondHalf.put(2, 48.40764331210191);
        cardToWeightSecondHalf.put(3, 49.27536231884058);
        cardToWeightSecondHalf.put(4, 56.423611111111114);
        cardToWeightSecondHalf.put(5, 37.03703703703704);
        cardToWeightSecondHalf.put(6, 59.57446808510638);
        cardToWeightSecondHalf.put(7, 41.86046511627907);
        cardToWeightSecondHalf.put(8, 41.86046511627907);
        cardToWeightSecondHalf.put(9, 42.857142857142854);
        cardToWeightSecondHalf.put(10, 45.16129032258065);
        cardToWeightSecondHalf.put(11, 45.05494505494506);
        cardToWeightSecondHalf.put(12, 50.0);
        cardToWeightSecondHalf.put(13, 43.38235294117647);
        cardToWeightSecondHalf.put(14, 52.38095238095238);
        cardToWeightSecondHalf.put(15, 43.1578947368421);
        cardToWeightSecondHalf.put(16, 62.06896551724138);
        cardToWeightSecondHalf.put(17, 57.17391304347826);
        cardToWeightSecondHalf.put(18, 52.23880597014925);
        cardToWeightSecondHalf.put(19, 47.96747967479675);
        cardToWeightSecondHalf.put(20, 28.0);
        cardToWeightSecondHalf.put(21, 40.54054054054054);
        cardToWeightSecondHalf.put(22, 43.31210191082803);
        cardToWeightSecondHalf.put(23, 52.666666666666664);
        cardToWeightSecondHalf.put(24, 50.0);
        cardToWeightSecondHalf.put(25, 50.41551246537396);
        cardToWeightSecondHalf.put(26, 39.77900552486188);
        cardToWeightSecondHalf.put(27, 40.0);
        cardToWeightSecondHalf.put(28, 40.0);
        cardToWeightSecondHalf.put(29, 38.46153846153846);
        cardToWeightSecondHalf.put(30, 45.714285714285715);
        cardToWeightSecondHalf.put(31, 35.0);
        cardToWeightSecondHalf.put(32, 36.61971830985915);
        cardToWeightSecondHalf.put(33, 48.507462686567166);
        cardToWeightSecondHalf.put(34, 37.254901960784316);
        cardToWeightSecondHalf.put(35, 60.34816247582205);
        cardToWeightSecondHalf.put(36, 40.0);
        cardToWeightSecondHalf.put(37, 49.00398406374502);
        cardToWeightSecondHalf.put(38, 47.41379310344828);
        cardToWeightSecondHalf.put(39, 58.97435897435897);
        cardToWeightSecondHalf.put(40, 53.44827586206897);
        cardToWeightSecondHalf.put(41, 43.75);
        cardToWeightSecondHalf.put(42, 53.13807531380753);
        cardToWeightSecondHalf.put(43, 43.75);
        cardToWeightSecondHalf.put(44, 50.55679287305122);
        cardToWeightSecondHalf.put(45, 49.60629921259842);
        cardToWeightSecondHalf.put(46, 55.67928730512249);
        cardToWeightSecondHalf.put(47, 37.2093023255814);
        cardToWeightSecondHalf.put(48, 50.0);
        cardToWeightSecondHalf.put(49, 46.42857142857143);
        cardToWeightSecondHalf.put(50, 41.666666666666664);
        cardToWeightSecondHalf.put(51, 37.125748502994014);
        cardToWeightSecondHalf.put(52, 57.69230769230769);
        cardToWeightSecondHalf.put(53, 48.77049180327869);
        cardToWeightSecondHalf.put(54, 58.333333333333336);
        cardToWeightSecondHalf.put(55, 55.55555555555556);
        cardToWeightSecondHalf.put(56, 43.50282485875706);
        cardToWeightSecondHalf.put(57, 45.23809523809524);
        cardToWeightSecondHalf.put(58, 43.13725490196079);
        cardToWeightSecondHalf.put(59, 45.714285714285715);
        cardToWeightSecondHalf.put(60, 54.83870967741935);
        cardToWeightSecondHalf.put(61, 55.0);
        cardToWeightSecondHalf.put(62, 54.651162790697676);
        cardToWeightSecondHalf.put(63, 58.76777251184834);
        cardToWeightSecondHalf.put(64, 54.43037974683544);
        cardToWeightSecondHalf.put(65, 55.81395348837209);
        cardToWeightSecondHalf.put(66, 50.0);
        cardToWeightSecondHalf.put(67, 47.63092269326683);
        cardToWeightSecondHalf.put(68, 46.59090909090909);
        cardToWeightSecondHalf.put(69, 54.42622950819672);
        cardToWeightSecondHalf.put(70, 52.549019607843135);
        cardToWeightSecondHalf.put(71, 43.884892086330936);
        cardToWeightSecondHalf.put(72, 53.54330708661417);
        cardToWeightSecondHalf.put(73, 47.03196347031963);
        cardToWeightSecondHalf.put(74, 50.9375);
        cardToWeightSecondHalf.put(75, 45.70446735395189);
        cardToWeightSecondHalf.put(76, 56.94760820045558);
        cardToWeightSecondHalf.put(77, 58.36909871244635);
        cardToWeightSecondHalf.put(78, 56.9364161849711);
        cardToWeightSecondHalf.put(79, 51.724137931034484);
        cardToWeightSecondHalf.put(80, 52.63157894736842);
        cardToWeightSecondHalf.put(81, 55.22727272727273);
        cardToWeightSecondHalf.put(82, 54.46584938704028);
        cardToWeightSecondHalf.put(83, 49.54128440366973);
        cardToWeightSecondHalf.put(84, 57.795698924731184);
        cardToWeightSecondHalf.put(85, 55.50122249388753);
        cardToWeightSecondHalf.put(86, 50.258175559380376);
        cardToWeightSecondHalf.put(87, 57.53899480069324);
        cardToWeightSecondHalf.put(88, 49.12280701754386);
        cardToWeightSecondHalf.put(89, 57.89473684210526);
        cardToWeightSecondHalf.put(90, 50.0);
        cardToWeightSecondHalf.put(91, 54.263565891472865);
        cardToWeightSecondHalf.put(92, 57.89473684210526);
        cardToWeightSecondHalf.put(93, 53.677621283255085);
        cardToWeightSecondHalf.put(94, 54.63414634146341);
        cardToWeightSecondHalf.put(95, 47.12643678160919);
        cardToWeightSecondHalf.put(96, 55.471698113207545);
        cardToWeightSecondHalf.put(97, 46.45669291338583);
        cardToWeightSecondHalf.put(98, 51.908396946564885);
        cardToWeightSecondHalf.put(99, 56.18279569892473);
        cardToWeightSecondHalf.put(100, 60.0);
        cardToWeightSecondHalf.put(101, 58.64332603938731);
        cardToWeightSecondHalf.put(102, 44.36619718309859);
        cardToWeightSecondHalf.put(103, 46.15384615384615);
        cardToWeightSecondHalf.put(104, 25.0);
        cardToWeightSecondHalf.put(105, 25.0);
        cardToWeightSecondHalf.put(106, 0.0);
        cardToWeightSecondHalf.put(107, 100.0);
        cardToWeightSecondHalf.put(108, 60.0);
        cardToWeightSecondHalf.put(109, 58.333333333333336);
        cardToWeightSecondHalf.put(110, 57.65407554671968);
        cardToWeightSecondHalf.put(111, 20.0);
        cardToWeightSecondHalf.put(112, 44.18604651162791);
        cardToWeightSecondHalf.put(113, 54.20168067226891);
        cardToWeightSecondHalf.put(114, 50.0);
        cardToWeightSecondHalf.put(115, 49.24242424242424);
        cardToWeightSecondHalf.put(116, 54.07801418439716);
        cardToWeightSecondHalf.put(117, 41.1764705882353);
        cardToWeightSecondHalf.put(118, 40.0);
        cardToWeightSecondHalf.put(119, 53.26086956521739);
        cardToWeightSecondHalf.put(120, 47.794117647058826);
        cardToWeightSecondHalf.put(121, 50.0);
        cardToWeightSecondHalf.put(122, 53.333333333333336);
        cardToWeightSecondHalf.put(123, 52.7027027027027);
        cardToWeightSecondHalf.put(124, 41.666666666666664);
        cardToWeightSecondHalf.put(125, 40.38461538461539);
        cardToWeightSecondHalf.put(126, 45.45454545454545);
        cardToWeightSecondHalf.put(127, 50.0);
        cardToWeightSecondHalf.put(128, 20.0);
        cardToWeightSecondHalf.put(129, 70.0);
        cardToWeightSecondHalf.put(130, 38.55421686746988);
        cardToWeightSecondHalf.put(131, 40.0);
        cardToWeightSecondHalf.put(132, 49.57983193277311);
        cardToWeightSecondHalf.put(133, 100.0);
        cardToWeightSecondHalf.put(134, 40.0);
        cardToWeightSecondHalf.put(135, 41.37931034482759);
        cardToWeightSecondHalf.put(136, 50.95693779904306);
        cardToWeightSecondHalf.put(137, 51.49863760217983);
        cardToWeightSecondHalf.put(138, 53.79537953795379);
        cardToWeightSecondHalf.put(139, 50.0);
        cardToWeightSecondHalf.put(140, 39.63963963963964);
        cardToWeightSecondHalf.put(141, 43.90243902439025);
        cardToWeightSecondHalf.put(142, 28.571428571428573);
        cardToWeightSecondHalf.put(143, 50.645994832041346);
        cardToWeightSecondHalf.put(144, 39.130434782608695);
        cardToWeightSecondHalf.put(145, 40.0);
        cardToWeightSecondHalf.put(146, 56.14754098360656);
        cardToWeightSecondHalf.put(147, 49.034749034749034);
        cardToWeightSecondHalf.put(148, 41.1764705882353);
        cardToWeightSecondHalf.put(149, 38.23529411764706);
        cardToWeightSecondHalf.put(150, 57.142857142857146);
        cardToWeightSecondHalf.put(151, 55.172413793103445);
        cardToWeightSecondHalf.put(152, 16.666666666666668);
        cardToWeightSecondHalf.put(153, 60.33402922755741);
        cardToWeightSecondHalf.put(154, 45.714285714285715);
        cardToWeightSecondHalf.put(155, 50.0);
        cardToWeightSecondHalf.put(156, 52.73224043715847);
        cardToWeightSecondHalf.put(157, 25.0);
        cardToWeightSecondHalf.put(158, 41.91616766467066);
        cardToWeightSecondHalf.put(159, 59.96275605214153);
        cardToWeightSecondHalf.put(160, 50.595238095238095);
        cardToWeightSecondHalf.put(161, 54.367201426024955);
        cardToWeightSecondHalf.put(162, 21.27659574468085);
        cardToWeightSecondHalf.put(163, 39.09090909090909);
        cardToWeightSecondHalf.put(164, 40.963855421686745);
        cardToWeightSecondHalf.put(165, 52.129817444219064);
        cardToWeightSecondHalf.put(166, 37.5);
        cardToWeightSecondHalf.put(167, 20.0);
        cardToWeightSecondHalf.put(168, 31.818181818181817);
        cardToWeightSecondHalf.put(169, 47.78761061946903);
        cardToWeightSecondHalf.put(170, 20.0);
        cardToWeightSecondHalf.put(171, 28.571428571428573);
        cardToWeightSecondHalf.put(172, 41.88034188034188);
        cardToWeightSecondHalf.put(173, 50.0);
        cardToWeightSecondHalf.put(174, 43.14720812182741);
        cardToWeightSecondHalf.put(175, 46.478873239436616);
        cardToWeightSecondHalf.put(176, 54.54545454545455);
        cardToWeightSecondHalf.put(177, 33.333333333333336);
        cardToWeightSecondHalf.put(178, 54.34380776340111);
        cardToWeightSecondHalf.put(179, 53.84615384615385);
        cardToWeightSecondHalf.put(180, 49.333333333333336);
        cardToWeightSecondHalf.put(181, 47.43589743589744);
        cardToWeightSecondHalf.put(182, 46.52777777777778);
        cardToWeightSecondHalf.put(183, 46.51162790697674);
        cardToWeightSecondHalf.put(184, 40.0);
        cardToWeightSecondHalf.put(185, 47.46835443037975);
        cardToWeightSecondHalf.put(186, 33.333333333333336);
        cardToWeightSecondHalf.put(187, 46.948356807511736);
        cardToWeightSecondHalf.put(188, 62.5);
        cardToWeightSecondHalf.put(189, 55.214723926380366);
        cardToWeightSecondHalf.put(190, 48.648648648648646);
        cardToWeightSecondHalf.put(191, 48.484848484848484);
        cardToWeightSecondHalf.put(192, 54.166666666666664);
        cardToWeightSecondHalf.put(193, 47.142857142857146);
        cardToWeightSecondHalf.put(194, 47.16981132075472);
        cardToWeightSecondHalf.put(195, 66.66666666666667);
        cardToWeightSecondHalf.put(196, 0.0);
        cardToWeightSecondHalf.put(197, 48.148148148148145);
        cardToWeightSecondHalf.put(198, 50.0);
        cardToWeightSecondHalf.put(199, 80.0);
        cardToWeightSecondHalf.put(200, 20.0);
        cardToWeightSecondHalf.put(201, 37.93103448275862);
        cardToWeightSecondHalf.put(202, 45.774647887323944);
        cardToWeightSecondHalf.put(203, 80.0);
        cardToWeightSecondHalf.put(204, 51.13636363636363);
        cardToWeightSecondHalf.put(205, 36.36363636363637);
        cardToWeightSecondHalf.put(206, 52.764976958525345);
        cardToWeightSecondHalf.put(207, 0.0);
        cardToWeightSecondHalf.put(208, 31.57894736842105);
        cardToWeightSecondHalf.put(209, 43.08510638297872);
        cardToWeightSecondHalf.put(210, 55.1948051948052);
        cardToWeightSecondHalf.put(211, 33.333333333333336);
        cardToWeightSecondHalf.put(212, 37.5);
        cardToWeightSecondHalf.put(213, 48.611111111111114);
        cardToWeightSecondHalf.put(214, 58.23754789272031);
        cardToWeightSecondHalf.put(215, 36.666666666666664);
        cardToWeightSecondHalf.put(216, 40.625);
        cardToWeightSecondHalf.put(217, 45.714285714285715);
        cardToWeightSecondHalf.put(218, 43.92156862745098);
        cardToWeightSecondHalf.put(219, 50.0);
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

    public Card getBestCardAsCard(MarsGame game, Player player, List<Card> cards, int turn) {
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
