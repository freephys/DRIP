
package org.drip.execution.tradingtime;

/*
 * -*- mode: java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 */

/*!
 * Copyright (C) 2017 Lakshmi Krishnamurthy
 * Copyright (C) 2016 Lakshmi Krishnamurthy
 * 
 *  This file is part of DRIP, a free-software/open-source library for buy/side financial/trading model
 *  	libraries targeting analysts and developers
 *  	https://lakshmidrip.github.io/DRIP/
 *  
 *  DRIP is composed of four main libraries:
 *  
 *  - DRIP Fixed Income - https://lakshmidrip.github.io/DRIP-Fixed-Income/
 *  - DRIP Asset Allocation - https://lakshmidrip.github.io/DRIP-Asset-Allocation/
 *  - DRIP Numerical Optimizer - https://lakshmidrip.github.io/DRIP-Numerical-Optimizer/
 *  - DRIP Statistical Learning - https://lakshmidrip.github.io/DRIP-Statistical-Learning/
 * 
 *  - DRIP Fixed Income: Library for Instrument/Trading Conventions, Treasury Futures/Options,
 *  	Funding/Forward/Overnight Curves, Multi-Curve Construction/Valuation, Collateral Valuation and XVA
 *  	Metric Generation, Calibration and Hedge Attributions, Statistical Curve Construction, Bond RV
 *  	Metrics, Stochastic Evolution and Option Pricing, Interest Rate Dynamics and Option Pricing, LMM
 *  	Extensions/Calibrations/Greeks, Algorithmic Differentiation, and Asset Backed Models and Analytics.
 * 
 *  - DRIP Asset Allocation: Library for model libraries for MPT framework, Black Litterman Strategy
 *  	Incorporator, Holdings Constraint, and Transaction Costs.
 * 
 *  - DRIP Numerical Optimizer: Library for Numerical Optimization and Spline Functionality.
 * 
 *  - DRIP Statistical Learning: Library for Statistical Evaluation and Machine Learning.
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
 * CoordinatedParticipationRateLinear implements the Coordinated Variation Version of the Linear
 *  Participation Rate Transaction Function as described in the "Trading Time" Model. The References are:
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
 * 	- Jones, C. M., G. Kaul, and M. L. Lipson (1994): Transactions, Volume, and Volatility, Review of
 * 		Financial Studies & (4) 631-651.
 * 
 * @author Lakshmi Krishnamurthy
 */

public class CoordinatedParticipationRateLinear implements
	org.drip.execution.profiletime.BackgroundParticipationRateLinear {
	private org.drip.function.definition.R1ToR1 _r1ToR1Volatility = null;
	private org.drip.execution.tradingtime.CoordinatedVariation _cv = null;

	/**
	 * CoordinatedParticipationRateLinear Constructor
	 * 
	 * @param cv The Coordinated Volatility/Liquidity Variation
	 * @param r1ToR1Volatility The R^1 -> R^1 Volatility Function
	 * 
	 * @throws java.lang.Exception Thrown if the Inputs are Invalid
	 */

	public CoordinatedParticipationRateLinear (
		final org.drip.execution.tradingtime.CoordinatedVariation cv,
		final org.drip.function.definition.R1ToR1 r1ToR1Volatility)
		throws java.lang.Exception
	{
		if (null == (_cv = cv) || null == (_r1ToR1Volatility = r1ToR1Volatility))
			throw new java.lang.Exception
				("CoordinatedParticipationRateLinear Constructor => Invalid Inputs");
	}

	/**
	 * Retrieve the Coordinated Variation Constraint
	 * 
	 * @return The Coordinated Variation Constraint
	 */

	public org.drip.execution.tradingtime.CoordinatedVariation variationConstraint()
	{
		return _cv;
	}

	@Override public org.drip.execution.impact.ParticipationRateLinear liquidityFunction (
		final double dblTime)
	{
		try {
			double dblVolatility = _r1ToR1Volatility.evaluate (dblTime);

			return org.drip.execution.impact.ParticipationRateLinear.SlopeOnly (_cv.invariant() /
				(dblVolatility * dblVolatility));
		} catch (java.lang.Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override public org.drip.execution.impact.TransactionFunction impactFunction (
		final double dblTime)
	{
		return liquidityFunction (dblTime);
	}

	@Override public org.drip.execution.impact.ParticipationRateLinear epochLiquidityFunction()
	{
		return liquidityFunction (0.);
	}

	@Override public org.drip.execution.impact.TransactionFunction epochImpactFunction()
	{
		return epochLiquidityFunction();
	}

	/**
	 * Compute the Volatility Function from the Liquidity Function
	 * 
	 * @return The R^1 -> R^1 Volatility Function
	 */

	public org.drip.function.definition.R1ToR1 volatilityFunction()
	{
		return new org.drip.function.definition.R1ToR1 (null) {
			@Override public double evaluate (
				final double dblTime)
				throws java.lang.Exception
			{
				org.drip.execution.impact.TransactionFunctionLinear tfl = liquidityFunction (dblTime);

				if (null == tfl)
					throw new java.lang.Exception
						("CoordinatedParticipationRateLinear::volatilityFunction::evaluate => Invalid Inputs");

				return java.lang.Math.sqrt (_cv.invariant() / tfl.slope());
			}
		};
	}
}
