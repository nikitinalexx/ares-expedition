package com.terraforming.ares.services.ai;

import com.terraforming.ares.model.Card;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 25.11.2022
 */
@Service
public class CardValueService {
    Map<Integer, Double> cardToWeightFirstHalf = new HashMap<>();
    Map<Integer, Double> cardToWeightSecondHalf = new HashMap<>();

    public CardValueService() {
        cardToWeightFirstHalf.put(1, 46.79300291545189);
        cardToWeightFirstHalf.put(2, 49.94026284348865);
        cardToWeightFirstHalf.put(3, 55.720823798627);
        cardToWeightFirstHalf.put(4, 71.42857142857143);
        cardToWeightFirstHalf.put(5, 55.19568151147099);
        cardToWeightFirstHalf.put(6, 88.23529411764706);
        cardToWeightFirstHalf.put(7, 50.91463414634146);
        cardToWeightFirstHalf.put(8, 50.943396226415096);
        cardToWeightFirstHalf.put(9, 46.38346727898967);
        cardToWeightFirstHalf.put(10, 53.07692307692308);
        cardToWeightFirstHalf.put(11, 50.520833333333336);
        cardToWeightFirstHalf.put(12, 0.0);
        cardToWeightFirstHalf.put(13, 52.008686210640604);
        cardToWeightFirstHalf.put(14, 100.0);
        cardToWeightFirstHalf.put(15, 51.2022630834512);
        cardToWeightFirstHalf.put(16, 53.771289537712896);
        cardToWeightFirstHalf.put(17, 51.76151761517615);
        cardToWeightFirstHalf.put(18, 50.78369905956113);
        cardToWeightFirstHalf.put(19, 39.33054393305439);
        cardToWeightFirstHalf.put(20, 49.87893462469734);
        cardToWeightFirstHalf.put(21, 48.77049180327869);
        cardToWeightFirstHalf.put(22, 46.69379450661241);
        cardToWeightFirstHalf.put(23, 60.117302052785924);
        cardToWeightFirstHalf.put(24, 48.959608323133416);
        cardToWeightFirstHalf.put(25, 55.811623246492985);
        cardToWeightFirstHalf.put(26, 62.177985948477755);
        cardToWeightFirstHalf.put(27, 46.13496932515337);
        cardToWeightFirstHalf.put(28, 52.997275204359674);
        cardToWeightFirstHalf.put(29, 52.14626391096979);
        cardToWeightFirstHalf.put(30, 52.58620689655172);
        cardToWeightFirstHalf.put(31, 54.83870967741935);
        cardToWeightFirstHalf.put(32, 50.0);
        cardToWeightFirstHalf.put(33, 66.66666666666667);
        cardToWeightFirstHalf.put(34, 44.19642857142857);
        cardToWeightFirstHalf.put(35, 75.40983606557377);
        cardToWeightFirstHalf.put(36, 48.15195071868583);
        cardToWeightFirstHalf.put(37, 57.12765957446808);
        cardToWeightFirstHalf.put(38, 47.395833333333336);
        cardToWeightFirstHalf.put(39, 66.66666666666667);
        cardToWeightFirstHalf.put(40, 53.74301675977654);
        cardToWeightFirstHalf.put(41, 47.635524798154556);
        cardToWeightFirstHalf.put(42, 50.37128712871287);
        cardToWeightFirstHalf.put(43, 58.25747724317295);
        cardToWeightFirstHalf.put(44, 56.50349650349651);
        cardToWeightFirstHalf.put(45, 50.471698113207545);
        cardToWeightFirstHalf.put(46, 68.64864864864865);
        cardToWeightFirstHalf.put(47, 47.523809523809526);
        cardToWeightFirstHalf.put(48, 45.21452145214521);
        cardToWeightFirstHalf.put(49, 43.01075268817204);
        cardToWeightFirstHalf.put(50, 52.77777777777778);
        cardToWeightFirstHalf.put(51, 58.21917808219178);
        cardToWeightFirstHalf.put(52, 43.93939393939394);
        cardToWeightFirstHalf.put(53, 62.116402116402114);
        cardToWeightFirstHalf.put(54, 46.413502109704645);
        cardToWeightFirstHalf.put(55, 42.42928452579035);
        cardToWeightFirstHalf.put(56, 45.0402144772118);
        cardToWeightFirstHalf.put(57, 52.60663507109005);
        cardToWeightFirstHalf.put(58, 53.46985210466439);
        cardToWeightFirstHalf.put(59, 49.29178470254958);
        cardToWeightFirstHalf.put(60, 60.957178841309826);
        cardToWeightFirstHalf.put(61, 49.608938547486034);
        cardToWeightFirstHalf.put(62, 38.372093023255815);
        cardToWeightFirstHalf.put(63, 42.398286937901496);
        cardToWeightFirstHalf.put(64, 50.47361299052774);
        cardToWeightFirstHalf.put(65, 47.5);
        cardToWeightFirstHalf.put(66, 62.5);
        cardToWeightFirstHalf.put(67, 54.54545454545455);
        cardToWeightFirstHalf.put(68, 50.0);
        cardToWeightFirstHalf.put(69, 55.57963163596966);
        cardToWeightFirstHalf.put(70, 51.95652173913044);
        cardToWeightFirstHalf.put(71, 50.0);
        cardToWeightFirstHalf.put(72, 48.739495798319325);
        cardToWeightFirstHalf.put(73, 50.42253521126761);
        cardToWeightFirstHalf.put(74, 54.06562054208274);
        cardToWeightFirstHalf.put(75, 61.666666666666664);
        cardToWeightFirstHalf.put(76, 58.0);
        cardToWeightFirstHalf.put(77, 52.0);
        cardToWeightFirstHalf.put(78, 56.11353711790393);
        cardToWeightFirstHalf.put(79, 0.0);
        cardToWeightFirstHalf.put(80, 53.43511450381679);
        cardToWeightFirstHalf.put(81, 50.825688073394495);
        cardToWeightFirstHalf.put(82, 64.51612903225806);
        cardToWeightFirstHalf.put(83, 54.535974973931175);
        cardToWeightFirstHalf.put(84, 56.88172043010753);
        cardToWeightFirstHalf.put(85, 54.07949790794979);
        cardToWeightFirstHalf.put(86, 25.0);
        cardToWeightFirstHalf.put(87, 48.23529411764706);
        cardToWeightFirstHalf.put(88, 48.76543209876543);
        cardToWeightFirstHalf.put(89, 53.01914580265096);
        cardToWeightFirstHalf.put(90, 0.0);
        cardToWeightFirstHalf.put(91, 56.68449197860963);
        cardToWeightFirstHalf.put(92, 25.0);
        cardToWeightFirstHalf.put(93, 52.06611570247934);
        cardToWeightFirstHalf.put(94, 57.69230769230769);
        cardToWeightFirstHalf.put(95, 47.31977818853974);
        cardToWeightFirstHalf.put(96, 53.88026607538803);
        cardToWeightFirstHalf.put(97, 48.294829482948295);
        cardToWeightFirstHalf.put(98, 51.515151515151516);
        cardToWeightFirstHalf.put(99, 52.705061082024436);
        cardToWeightFirstHalf.put(100, 47.747747747747745);
        cardToWeightFirstHalf.put(101, 56.01965601965602);
        cardToWeightFirstHalf.put(102, 56.33187772925764);
        cardToWeightFirstHalf.put(103, 54.33376455368693);
        cardToWeightFirstHalf.put(104, 46.65841584158416);
        cardToWeightFirstHalf.put(105, 49.438202247191015);
        cardToWeightFirstHalf.put(106, 55.33980582524272);
        cardToWeightFirstHalf.put(107, 31.25);
        cardToWeightFirstHalf.put(108, 50.130208333333336);
        cardToWeightFirstHalf.put(109, 51.666666666666664);
        cardToWeightFirstHalf.put(110, 51.55038759689923);
        cardToWeightFirstHalf.put(111, 49.741602067183464);
        cardToWeightFirstHalf.put(112, 47.77327935222672);
        cardToWeightFirstHalf.put(113, 52.254641909814325);
        cardToWeightFirstHalf.put(114, 52.62172284644195);
        cardToWeightFirstHalf.put(115, 48.26302729528536);
        cardToWeightFirstHalf.put(116, 49.375);
        cardToWeightFirstHalf.put(117, 60.97560975609756);
        cardToWeightFirstHalf.put(118, 51.056338028169016);
        cardToWeightFirstHalf.put(119, 54.67479674796748);
        cardToWeightFirstHalf.put(120, 53.13807531380753);
        cardToWeightFirstHalf.put(121, 50.68493150684932);
        cardToWeightFirstHalf.put(122, 48.69565217391305);
        cardToWeightFirstHalf.put(123, 53.37662337662338);
        cardToWeightFirstHalf.put(124, 47.8125);
        cardToWeightFirstHalf.put(125, 51.52671755725191);
        cardToWeightFirstHalf.put(126, 46.70750382848392);
        cardToWeightFirstHalf.put(127, 54.864433811802236);
        cardToWeightFirstHalf.put(128, 49.25650557620818);
        cardToWeightFirstHalf.put(129, 51.92069392812887);
        cardToWeightFirstHalf.put(130, 53.59765051395007);
        cardToWeightFirstHalf.put(131, 57.333333333333336);
        cardToWeightFirstHalf.put(132, 40.0);
        cardToWeightFirstHalf.put(133, 0.0);
        cardToWeightFirstHalf.put(134, 52.58964143426295);
        cardToWeightFirstHalf.put(135, 46.37096774193548);
        cardToWeightFirstHalf.put(136, 49.25);
        cardToWeightFirstHalf.put(137, 60.22727272727273);
        cardToWeightFirstHalf.put(138, 54.025044722719144);
        cardToWeightFirstHalf.put(139, 52.51798561151079);
        cardToWeightFirstHalf.put(140, 49.799196787148595);
        cardToWeightFirstHalf.put(141, 50.98039215686274);
        cardToWeightFirstHalf.put(142, 47.22222222222222);
        cardToWeightFirstHalf.put(143, 48.4304932735426);
        cardToWeightFirstHalf.put(144, 52.74431057563588);
        cardToWeightFirstHalf.put(145, 49.65635738831615);
        cardToWeightFirstHalf.put(146, 52.05761316872428);
        cardToWeightFirstHalf.put(147, 49.74293059125964);
        cardToWeightFirstHalf.put(148, 47.12990936555891);
        cardToWeightFirstHalf.put(149, 56.672158154859964);
        cardToWeightFirstHalf.put(150, 52.916666666666664);
        cardToWeightFirstHalf.put(151, 51.60390516039052);
        cardToWeightFirstHalf.put(152, 50.82212257100149);
        cardToWeightFirstHalf.put(153, 54.88721804511278);
        cardToWeightFirstHalf.put(154, 100.0);
        cardToWeightFirstHalf.put(155, 51.856763925729446);
        cardToWeightFirstHalf.put(156, 54.15282392026578);
        cardToWeightFirstHalf.put(157, 52.94117647058823);
        cardToWeightFirstHalf.put(158, 50.829562594268474);
        cardToWeightFirstHalf.put(159, 69.81132075471699);
        cardToWeightFirstHalf.put(160, 48.95104895104895);
        cardToWeightFirstHalf.put(161, 61.53846153846154);
        cardToWeightFirstHalf.put(162, 50.57142857142857);
        cardToWeightFirstHalf.put(163, 48.58870967741935);
        cardToWeightFirstHalf.put(164, 59.527326440177255);
        cardToWeightFirstHalf.put(165, 51.93929173693086);
        cardToWeightFirstHalf.put(166, 51.702127659574465);
        cardToWeightFirstHalf.put(167, 46.8562874251497);
        cardToWeightFirstHalf.put(168, 46.15384615384615);
        cardToWeightFirstHalf.put(169, 43.80165289256198);
        cardToWeightFirstHalf.put(170, 55.79322638146168);
        cardToWeightFirstHalf.put(171, 51.09489051094891);
        cardToWeightFirstHalf.put(172, 63.63636363636363);
        cardToWeightFirstHalf.put(173, 54.51761102603369);
        cardToWeightFirstHalf.put(174, 53.608247422680414);
        cardToWeightFirstHalf.put(175, 49.45504087193461);
        cardToWeightFirstHalf.put(176, 51.12903225806452);
        cardToWeightFirstHalf.put(177, 47.80361757105943);
        cardToWeightFirstHalf.put(178, 59.310344827586206);
        cardToWeightFirstHalf.put(179, 48.62385321100918);
        cardToWeightFirstHalf.put(180, 51.703406813627254);
        cardToWeightFirstHalf.put(181, 50.77720207253886);
        cardToWeightFirstHalf.put(182, 53.90279823269514);
        cardToWeightFirstHalf.put(183, 59.25925925925926);
        cardToWeightFirstHalf.put(184, 56.38766519823788);
        cardToWeightFirstHalf.put(185, 45.57142857142857);
        cardToWeightFirstHalf.put(186, 46.40122511485452);
        cardToWeightFirstHalf.put(187, 47.72727272727273);
        cardToWeightFirstHalf.put(188, 49.51298701298701);
        cardToWeightFirstHalf.put(189, 49.13194444444444);
        cardToWeightFirstHalf.put(190, 52.83018867924528);
        cardToWeightFirstHalf.put(191, 57.35294117647059);
        cardToWeightFirstHalf.put(192, 54.18803418803419);
        cardToWeightFirstHalf.put(193, 48.507462686567166);
        cardToWeightFirstHalf.put(194, 48.66863905325444);
        cardToWeightFirstHalf.put(195, 48.47826086956522);
        cardToWeightFirstHalf.put(196, 50.258175559380376);
        cardToWeightFirstHalf.put(197, 35.8974358974359);
        cardToWeightFirstHalf.put(198, 100.0);
        cardToWeightFirstHalf.put(199, 50.0);
        cardToWeightFirstHalf.put(200, 50.0);
        cardToWeightFirstHalf.put(201, 52.41057542768274);
        cardToWeightFirstHalf.put(202, 61.5819209039548);
        cardToWeightFirstHalf.put(203, 47.08029197080292);
        cardToWeightFirstHalf.put(204, 51.241134751773046);
        cardToWeightFirstHalf.put(205, 51.578947368421055);
        cardToWeightFirstHalf.put(206, 52.990033222591364);
        cardToWeightFirstHalf.put(207, 47.87234042553192);
        cardToWeightFirstHalf.put(208, 47.41379310344828);
        cardToWeightFirstHalf.put(209, 50.13623978201635);
        cardToWeightFirstHalf.put(210, 64.54545454545455);
        cardToWeightFirstHalf.put(211, 51.100628930817614);
        cardToWeightFirstHalf.put(212, 52.84552845528455);
        cardToWeightFirstHalf.put(213, 63.65217391304348);
        cardToWeightFirstHalf.put(214, 57.70750988142292);
        cardToWeightFirstHalf.put(215, 57.78894472361809);
        cardToWeightFirstHalf.put(216, 42.34104046242775);
        cardToWeightFirstHalf.put(217, 47.32142857142857);
        cardToWeightFirstHalf.put(218, 50.14367816091954);
        cardToWeightFirstHalf.put(219, 53.475935828877006);
        cardToWeightSecondHalf.put(1, 51.95154777927322);
        cardToWeightSecondHalf.put(2, 49.21875);
        cardToWeightSecondHalf.put(3, 46.33152173913044);
        cardToWeightSecondHalf.put(4, 61.83574879227053);
        cardToWeightSecondHalf.put(5, 48.793565683646115);
        cardToWeightSecondHalf.put(6, 66.0933660933661);
        cardToWeightSecondHalf.put(7, 47.3756906077348);
        cardToWeightSecondHalf.put(8, 58.8477366255144);
        cardToWeightSecondHalf.put(9, 42.837273991655074);
        cardToWeightSecondHalf.put(10, 48.536209553158706);
        cardToWeightSecondHalf.put(11, 46.38922888616891);
        cardToWeightSecondHalf.put(12, 55.0);
        cardToWeightSecondHalf.put(13, 48.99425287356322);
        cardToWeightSecondHalf.put(14, 49.01610017889087);
        cardToWeightSecondHalf.put(15, 49.03703703703704);
        cardToWeightSecondHalf.put(16, 48.25072886297376);
        cardToWeightSecondHalf.put(17, 50.427350427350426);
        cardToWeightSecondHalf.put(18, 50.46875);
        cardToWeightSecondHalf.put(19, 49.66139954853273);
        cardToWeightSecondHalf.put(20, 50.44117647058823);
        cardToWeightSecondHalf.put(21, 51.71919770773639);
        cardToWeightSecondHalf.put(22, 45.45454545454545);
        cardToWeightSecondHalf.put(23, 57.001414427157);
        cardToWeightSecondHalf.put(24, 51.88284518828452);
        cardToWeightSecondHalf.put(25, 47.38330975954738);
        cardToWeightSecondHalf.put(26, 47.56410256410256);
        cardToWeightSecondHalf.put(27, 51.785714285714285);
        cardToWeightSecondHalf.put(28, 53.910614525139664);
        cardToWeightSecondHalf.put(29, 51.12676056338028);
        cardToWeightSecondHalf.put(30, 55.65669700910273);
        cardToWeightSecondHalf.put(31, 48.83203559510567);
        cardToWeightSecondHalf.put(32, 50.0);
        cardToWeightSecondHalf.put(33, 59.946236559139784);
        cardToWeightSecondHalf.put(34, 49.658002735978116);
        cardToWeightSecondHalf.put(35, 64.8695652173913);
        cardToWeightSecondHalf.put(36, 47.82051282051282);
        cardToWeightSecondHalf.put(37, 47.80701754385965);
        cardToWeightSecondHalf.put(38, 50.34387895460798);
        cardToWeightSecondHalf.put(39, 57.84469096671949);
        cardToWeightSecondHalf.put(40, 48.24191279887482);
        cardToWeightSecondHalf.put(41, 51.87760778859527);
        cardToWeightSecondHalf.put(42, 44.4104134762634);
        cardToWeightSecondHalf.put(43, 54.13223140495868);
        cardToWeightSecondHalf.put(44, 51.900393184796854);
        cardToWeightSecondHalf.put(45, 48.53556485355649);
        cardToWeightSecondHalf.put(46, 56.60919540229885);
        cardToWeightSecondHalf.put(47, 49.928876244665716);
        cardToWeightSecondHalf.put(48, 44.00656814449918);
        cardToWeightSecondHalf.put(49, 46.831530139103556);
        cardToWeightSecondHalf.put(50, 47.89823008849557);
        cardToWeightSecondHalf.put(51, 50.554016620498615);
        cardToWeightSecondHalf.put(52, 52.63157894736842);
        cardToWeightSecondHalf.put(53, 50.588235294117645);
        cardToWeightSecondHalf.put(54, 51.36239782016349);
        cardToWeightSecondHalf.put(55, 49.66622162883845);
        cardToWeightSecondHalf.put(56, 47.08520179372197);
        cardToWeightSecondHalf.put(57, 48.033373063170444);
        cardToWeightSecondHalf.put(58, 47.87701317715959);
        cardToWeightSecondHalf.put(59, 56.276445698166434);
        cardToWeightSecondHalf.put(60, 50.416666666666664);
        cardToWeightSecondHalf.put(61, 52.78969957081545);
        cardToWeightSecondHalf.put(62, 55.720053835800805);
        cardToWeightSecondHalf.put(63, 54.13105413105413);
        cardToWeightSecondHalf.put(64, 48.16901408450704);
        cardToWeightSecondHalf.put(65, 56.1712846347607);
        cardToWeightSecondHalf.put(66, 50.08237232289951);
        cardToWeightSecondHalf.put(67, 52.65017667844523);
        cardToWeightSecondHalf.put(68, 50.495049504950494);
        cardToWeightSecondHalf.put(69, 49.12751677852349);
        cardToWeightSecondHalf.put(70, 51.64383561643836);
        cardToWeightSecondHalf.put(71, 49.06204906204906);
        cardToWeightSecondHalf.put(72, 53.14861460957179);
        cardToWeightSecondHalf.put(73, 48.98911353032659);
        cardToWeightSecondHalf.put(74, 49.64936886395512);
        cardToWeightSecondHalf.put(75, 51.973684210526315);
        cardToWeightSecondHalf.put(76, 54.3778801843318);
        cardToWeightSecondHalf.put(77, 56.9204152249135);
        cardToWeightSecondHalf.put(78, 54.82517482517483);
        cardToWeightSecondHalf.put(79, 52.05811138014528);
        cardToWeightSecondHalf.put(80, 56.2406015037594);
        cardToWeightSecondHalf.put(81, 54.269972451790636);
        cardToWeightSecondHalf.put(82, 56.14366729678639);
        cardToWeightSecondHalf.put(83, 50.30902348578492);
        cardToWeightSecondHalf.put(84, 53.31599479843953);
        cardToWeightSecondHalf.put(85, 53.10596833130329);
        cardToWeightSecondHalf.put(86, 54.03508771929825);
        cardToWeightSecondHalf.put(87, 57.79661016949152);
        cardToWeightSecondHalf.put(88, 49.178082191780824);
        cardToWeightSecondHalf.put(89, 51.83098591549296);
        cardToWeightSecondHalf.put(90, 47.28260869565217);
        cardToWeightSecondHalf.put(91, 56.57686212361331);
        cardToWeightSecondHalf.put(92, 52.19298245614035);
        cardToWeightSecondHalf.put(93, 57.21311475409836);
        cardToWeightSecondHalf.put(94, 64.30155210643015);
        cardToWeightSecondHalf.put(95, 51.496062992125985);
        cardToWeightSecondHalf.put(96, 49.39467312348668);
        cardToWeightSecondHalf.put(97, 50.45871559633027);
        cardToWeightSecondHalf.put(98, 48.582995951417004);
        cardToWeightSecondHalf.put(99, 56.835443037974684);
        cardToWeightSecondHalf.put(100, 58.333333333333336);
        cardToWeightSecondHalf.put(101, 52.38784370477569);
        cardToWeightSecondHalf.put(102, 47.59887005649718);
        cardToWeightSecondHalf.put(103, 47.54420432220039);
        cardToWeightSecondHalf.put(104, 49.473684210526315);
        cardToWeightSecondHalf.put(105, 49.90583804143126);
        cardToWeightSecondHalf.put(106, 48.49498327759197);
        cardToWeightSecondHalf.put(107, 48.46938775510204);
        cardToWeightSecondHalf.put(108, 44.72361809045226);
        cardToWeightSecondHalf.put(109, 44.935543278084715);
        cardToWeightSecondHalf.put(110, 52.88270377733598);
        cardToWeightSecondHalf.put(111, 44.18604651162791);
        cardToWeightSecondHalf.put(112, 54.166666666666664);
        cardToWeightSecondHalf.put(113, 51.42857142857143);
        cardToWeightSecondHalf.put(114, 46.93877551020408);
        cardToWeightSecondHalf.put(115, 51.52542372881356);
        cardToWeightSecondHalf.put(116, 57.39795918367347);
        cardToWeightSecondHalf.put(117, 52.76752767527675);
        cardToWeightSecondHalf.put(118, 52.91005291005291);
        cardToWeightSecondHalf.put(119, 47.932330827067666);
        cardToWeightSecondHalf.put(120, 52.78350515463917);
        cardToWeightSecondHalf.put(121, 51.34649910233393);
        cardToWeightSecondHalf.put(122, 50.83179297597042);
        cardToWeightSecondHalf.put(123, 49.909909909909906);
        cardToWeightSecondHalf.put(124, 52.45009074410164);
        cardToWeightSecondHalf.put(125, 52.51937984496124);
        cardToWeightSecondHalf.put(126, 49.6124031007752);
        cardToWeightSecondHalf.put(127, 49.557522123893804);
        cardToWeightSecondHalf.put(128, 46.96969696969697);
        cardToWeightSecondHalf.put(129, 48.611111111111114);
        cardToWeightSecondHalf.put(130, 51.13438045375218);
        cardToWeightSecondHalf.put(131, 45.86846543001686);
        cardToWeightSecondHalf.put(132, 51.9327731092437);
        cardToWeightSecondHalf.put(133, 55.04587155963303);
        cardToWeightSecondHalf.put(134, 44.44444444444444);
        cardToWeightSecondHalf.put(135, 49.803149606299215);
        cardToWeightSecondHalf.put(136, 46.43423137876387);
        cardToWeightSecondHalf.put(137, 51.43824027072758);
        cardToWeightSecondHalf.put(138, 49.034749034749034);
        cardToWeightSecondHalf.put(139, 51.4792899408284);
        cardToWeightSecondHalf.put(140, 48.0672268907563);
        cardToWeightSecondHalf.put(141, 51.61839863713799);
        cardToWeightSecondHalf.put(142, 47.11246200607903);
        cardToWeightSecondHalf.put(143, 48.29821717990276);
        cardToWeightSecondHalf.put(144, 47.42765273311897);
        cardToWeightSecondHalf.put(145, 54.52865064695009);
        cardToWeightSecondHalf.put(146, 51.676528599605525);
        cardToWeightSecondHalf.put(147, 48.55195911413969);
        cardToWeightSecondHalf.put(148, 52.24913494809689);
        cardToWeightSecondHalf.put(149, 51.42857142857143);
        cardToWeightSecondHalf.put(150, 48.34307992202729);
        cardToWeightSecondHalf.put(151, 53.10457516339869);
        cardToWeightSecondHalf.put(152, 48.36363636363637);
        cardToWeightSecondHalf.put(153, 54.845360824742265);
        cardToWeightSecondHalf.put(154, 51.21951219512195);
        cardToWeightSecondHalf.put(155, 49.83766233766234);
        cardToWeightSecondHalf.put(156, 51.64473684210526);
        cardToWeightSecondHalf.put(157, 52.27920227920228);
        cardToWeightSecondHalf.put(158, 48.34437086092715);
        cardToWeightSecondHalf.put(159, 61.40350877192982);
        cardToWeightSecondHalf.put(160, 53.30948121645796);
        cardToWeightSecondHalf.put(161, 51.7162471395881);
        cardToWeightSecondHalf.put(162, 51.63636363636363);
        cardToWeightSecondHalf.put(163, 51.74825174825175);
        cardToWeightSecondHalf.put(164, 48.68189806678383);
        cardToWeightSecondHalf.put(165, 53.03030303030303);
        cardToWeightSecondHalf.put(166, 47.387387387387385);
        cardToWeightSecondHalf.put(167, 44.5);
        cardToWeightSecondHalf.put(168, 44.386873920552674);
        cardToWeightSecondHalf.put(169, 49.787234042553195);
        cardToWeightSecondHalf.put(170, 51.301115241635685);
        cardToWeightSecondHalf.put(171, 48.4375);
        cardToWeightSecondHalf.put(172, 51.729559748427675);
        cardToWeightSecondHalf.put(173, 49.66442953020134);
        cardToWeightSecondHalf.put(174, 49.76152623211447);
        cardToWeightSecondHalf.put(175, 50.08347245409015);
        cardToWeightSecondHalf.put(176, 44.84848484848485);
        cardToWeightSecondHalf.put(177, 49.288256227758005);
        cardToWeightSecondHalf.put(178, 57.16981132075472);
        cardToWeightSecondHalf.put(179, 47.45762711864407);
        cardToWeightSecondHalf.put(180, 49.152542372881356);
        cardToWeightSecondHalf.put(181, 52.27272727272727);
        cardToWeightSecondHalf.put(182, 48.80733944954128);
        cardToWeightSecondHalf.put(183, 49.72677595628415);
        cardToWeightSecondHalf.put(184, 51.20147874306839);
        cardToWeightSecondHalf.put(185, 48.65319865319865);
        cardToWeightSecondHalf.put(186, 44.57627118644068);
        cardToWeightSecondHalf.put(187, 52.03883495145631);
        cardToWeightSecondHalf.put(188, 48.960302457466916);
        cardToWeightSecondHalf.put(189, 49.393414211438476);
        cardToWeightSecondHalf.put(190, 48.43205574912892);
        cardToWeightSecondHalf.put(191, 50.445632798573975);
        cardToWeightSecondHalf.put(192, 48.0);
        cardToWeightSecondHalf.put(193, 52.42537313432836);
        cardToWeightSecondHalf.put(194, 50.08517887563884);
        cardToWeightSecondHalf.put(195, 44.95049504950495);
        cardToWeightSecondHalf.put(196, 48.03921568627451);
        cardToWeightSecondHalf.put(197, 50.8637236084453);
        cardToWeightSecondHalf.put(198, 52.472527472527474);
        cardToWeightSecondHalf.put(199, 47.686116700201204);
        cardToWeightSecondHalf.put(200, 52.713178294573645);
        cardToWeightSecondHalf.put(201, 51.07913669064748);
        cardToWeightSecondHalf.put(202, 48.86128364389234);
        cardToWeightSecondHalf.put(203, 49.90403071017275);
        cardToWeightSecondHalf.put(204, 52.45283018867924);
        cardToWeightSecondHalf.put(205, 49.916247906197654);
        cardToWeightSecondHalf.put(206, 52.59391771019678);
        cardToWeightSecondHalf.put(207, 49.369085173501574);
        cardToWeightSecondHalf.put(208, 50.23255813953488);
        cardToWeightSecondHalf.put(209, 48.994082840236686);
        cardToWeightSecondHalf.put(210, 50.5938242280285);
        cardToWeightSecondHalf.put(211, 52.18181818181818);
        cardToWeightSecondHalf.put(212, 51.13268608414239);
        cardToWeightSecondHalf.put(213, 52.94117647058823);
        cardToWeightSecondHalf.put(214, 56.58914728682171);
        cardToWeightSecondHalf.put(215, 51.74311926605505);
        cardToWeightSecondHalf.put(216, 52.7363184079602);
        cardToWeightSecondHalf.put(217, 50.45278137128072);
        cardToWeightSecondHalf.put(218, 48.375451263537904);
        cardToWeightSecondHalf.put(219, 49.375);
    }

    public static final double MIDDLE_TURN = 19.245;

    public Integer getWorstCard(List<Integer> cards, int turn) {
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
            double worth = getCardWorth(cards.get(i), firstHalfCoefficient, secondHalfCoefficient);
            if (worth < worstCard) {
                worstCard = worth;
                worstCardIndex = i;
            }
        }

        return cards.get(worstCardIndex);
    }

    public Card getBestCard(List<Card> cards, int turn) {
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
        int bestCardIndex = 0;

        for (int i = 0; i < cards.size(); i++) {
            double worth = getCardWorth(cards.get(i).getId(), firstHalfCoefficient, secondHalfCoefficient);
            if (worth >= bestCard) {
                bestCard = worth;
                bestCardIndex = i;
            }
        }

        return cards.get(bestCardIndex);
    }

    public Integer getBestCardId(List<Integer> cards, int turn) {
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
        int bestCardIndex = 0;

        for (int i = 0; i < cards.size(); i++) {
            double worth = getCardWorth(cards.get(i), firstHalfCoefficient, secondHalfCoefficient);
            if (worth >= bestCard) {
                bestCard = worth;
                bestCardIndex = i;
            }
        }

        return cards.get(bestCardIndex);
    }

    private double getCardWorth(Integer card, double firstHalfCoefficient, double secondHalfCoefficient) {
        return cardToWeightFirstHalf.get(card) * firstHalfCoefficient + cardToWeightSecondHalf.get(card) * secondHalfCoefficient;
    }

}
