package com.terraforming.ares.services.agent;

import com.terraforming.ares.model.StandardProjectType;

import java.util.List;
import java.util.Map;

/**
 * One legal third-phase spend: a standard project, a resource conversion, converting one Helion heat, or stop.
 *
 * @param kind        what to execute; {@link StandardActionDecisions#STOP} means take no spend
 * @param projectType set for a standard project; null otherwise
 * @param input       Cargo Ships (or empty) for infrastructure raises; empty otherwise
 */
public record StandardActionChoice(int kind, StandardProjectType projectType, Map<Integer, List<Integer>> input) {
}
