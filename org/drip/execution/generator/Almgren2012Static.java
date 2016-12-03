
package org.drip.execution.generator;

/*
 * -*- mode: java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 */

/*!
 * Copyright (C) 2016 Lakshmi Krishnamurthy
 * 
 *  This file is part of DRIP, a free-software/open-source library for fixed income analysts and developers -
 * 		http://www.credit-trader.org/Begin.html
 * 
 *  DRIP is a free, full featured, fixed income rates, credit, and FX analytics library with a focus towards
 *  	pricing/valuation, risk, and market making.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *   	you may not use this file except in compliance with the License.
 *   
 *  You may obtain a copy of the License at
 *  	http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  	distributed under the License is distributed on an "AS IS" BASIS,
 *  	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  
 *  See the License for the specific language governing permissions and
 *  	limitations under the License.
 */

/**
 * Almgren2012Static contains the Continuous Version of the Discrete Trading Trajectory generated by the
 *  Almgren and Chriss (2000) Scheme under the Criterion of No-Drift. The References are:
 * 
 * 	- Almgren, R. F., and N. Chriss (2000): Optimal Execution of Portfolio Transactions, Journal of Risk 3
 * 		(2) 5-39.
 *
 * 	- Almgren, R. F. (2009): Optimal Trading in a Dynamic Market
 * 		https://www.math.nyu.edu/financial_mathematics/content/02_financial/2009-2.pdf.
 *
 * 	- Almgren, R. F. (2012): Optimal Trading with Stochastic Liquidity and Volatility, SIAM Journal of
 * 		Financial Mathematics  3 (1) 163-181.
 * 
 * 	- Geman, H., D. B. Madan, and M. Yor (2001): Time Changes for Levy Processes, Mathematical Finance 11 (1)
 * 		79-96.
 * 
 * 	- Walia, N. (2006): Optimal Trading: Dynamic Stock Liquidation Strategies, Senior Thesis, Princeton
 * 		University.
 * 
 * @author Lakshmi Krishnamurthy
 */

public class Almgren2012Static extends org.drip.execution.generator.OptimalTrajectorySchemeContinuous {

	/**
	 * Create the Standard Almgren2003PowerImpact Instance
	 * 
	 * @param dblStartHoldings Trajectory Start Holdings
	 * @param dblFinishTime Trajectory Finish Time
	 * @param lep The Linear Impact Expectation Parameters
	 * @param dblRiskAversion The Risk Aversion Parameter
	 * 
	 * @return The Almgren2012Static Instance
	 */

	public static final Almgren2012Static Standard (
		final double dblStartHoldings,
		final double dblFinishTime,
		final org.drip.execution.dynamics.LinearExpectationParameters lep,
		final double dblRiskAversion)
	{
		try {
			return new Almgren2012Static (new org.drip.execution.strategy.OrderSpecification
				(dblStartHoldings, dblFinishTime), lep, new
					org.drip.execution.risk.MeanVarianceObjectiveUtility (dblRiskAversion));
		} catch (java.lang.Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private Almgren2012Static (
		final org.drip.execution.strategy.OrderSpecification os,
		final org.drip.execution.dynamics.LinearExpectationParameters lep,
		final org.drip.execution.risk.MeanVarianceObjectiveUtility mvou)
		throws java.lang.Exception
	{
		super (os, lep, mvou);
	}

	@Override public org.drip.execution.optimum.EfficientTradingTrajectory generate()
	{
		org.drip.execution.dynamics.LinearExpectationParameters lep =
			(org.drip.execution.dynamics.LinearExpectationParameters) priceWalkParameters();

		org.drip.execution.impact.TransactionFunctionLinear tflTemporaryExpectation =
			lep.linearTemporaryExpectation();

		final double dblSigma = lep.arithmeticPriceDynamicsSettings().volatility();

		final double dblEta = tflTemporaryExpectation.slope();

		final double dblKappa = java.lang.Math.sqrt (((org.drip.execution.risk.MeanVarianceObjectiveUtility)
			objectiveUtility()).riskAversion() * dblSigma * dblSigma / dblEta);

		org.drip.execution.strategy.OrderSpecification os = orderSpecification();

		final double dblT = os.maxExecutionTime();

		final double dblX = os.size();

		final org.drip.function.definition.R1ToR1 r1ToR1Holdings = new org.drip.function.definition.R1ToR1
			(null) {
			@Override public double evaluate (
				final double dblS)
				throws java.lang.Exception
			{
				if (!org.drip.quant.common.NumberUtil.IsValid (dblS))
					throw new java.lang.Exception
						("Almgren2012Static::Holdings::evaluate => Invalid Inputs");

				return java.lang.Math.sinh (dblKappa * (dblT - dblS)) / java.lang.Math.sinh (dblKappa * dblT)
					* dblX;
			}
		};

		org.drip.function.definition.R1ToR1 r1ToR1TradeRate = new org.drip.function.definition.R1ToR1 (null)
		{
			@Override public double evaluate (
				final double dblS)
				throws java.lang.Exception
			{
				if (!org.drip.quant.common.NumberUtil.IsValid (dblS))
					throw new java.lang.Exception
						("Almgren2012Static::TradeRate::evaluate => Invalid Inputs");

				return dblKappa * dblX * java.lang.Math.cosh (dblKappa * (dblT - dblS)) / java.lang.Math.sinh
					(dblKappa * dblT);
			}
		};

		final org.drip.function.definition.R1ToR1 r1ToR1TransactionCostExpectationRate = new
			org.drip.function.definition.R1ToR1 (null) {
			@Override public double evaluate (
				final double dblTime)
				throws java.lang.Exception
			{
				double dblTradeRate = r1ToR1Holdings.derivative (dblTime, 1);

				return dblEta * dblEta * dblTradeRate * dblTradeRate;
			}
		};

		org.drip.function.definition.R1ToR1 r1ToR1TransactionCostExpectation = new
			org.drip.function.definition.R1ToR1 (null) {
			@Override public double evaluate (
				final double dblTime)
				throws java.lang.Exception
			{
				return r1ToR1TransactionCostExpectationRate.integrate (dblTime, dblT);
			}
		};

		final org.drip.function.definition.R1ToR1 r1ToR1TransactionCostVarianceRate = new
			org.drip.function.definition.R1ToR1 (null) {
			@Override public double evaluate (
				final double dblTime)
				throws java.lang.Exception
			{
				double dblHoldings = r1ToR1Holdings.evaluate (dblTime);

				return dblSigma * dblSigma * dblHoldings * dblHoldings;
			}
		};

		org.drip.function.definition.R1ToR1 r1ToR1TransactionCostVariance = new
			org.drip.function.definition.R1ToR1 (null) {
			@Override public double evaluate (
				final double dblTime)
				throws java.lang.Exception
			{
				return r1ToR1TransactionCostVarianceRate.integrate (dblTime, dblT);
			}
		};

		try {
			return new org.drip.execution.optimum.EfficientTradingTrajectoryContinuous (dblT, dblEta *
				dblKappa * dblX * dblX / java.lang.Math.tanh (dblKappa * dblT),
					r1ToR1TransactionCostExpectation.evaluate (0.), 1. / dblKappa, r1ToR1Holdings,
						r1ToR1TradeRate, r1ToR1TransactionCostExpectation,
							r1ToR1TransactionCostVariance);
		} catch (java.lang.Exception e) {
			e.printStackTrace();
		}

		return null;
	}
}
