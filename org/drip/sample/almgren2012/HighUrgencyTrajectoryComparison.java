
package org.drip.sample.almgren2012;

import org.drip.execution.dynamics.*;
import org.drip.execution.impact.ParticipationRateLinear;
import org.drip.execution.nonadaptive.*;
import org.drip.execution.optimum.EfficientTradingTrajectoryContinuous;
import org.drip.execution.parameters.*;
import org.drip.execution.profiletime.UniformParticipationRateLinear;
import org.drip.function.definition.R1ToR1;
import org.drip.function.r1tor1.FlatUnivariate;
import org.drip.quant.common.FormatUtil;
import org.drip.service.env.EnvManager;

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
 * HighUrgencyTrajectoryComparison compares the Static Continuous Trading Trajectory generated by the Almgren
 *  and Chriss (2012) Scheme against the High Urgency Asymptote Version. The References are:
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

public class HighUrgencyTrajectoryComparison {

	public static void main (
		final String[] astrArgs)
		throws Exception
	{
		EnvManager.InitEnv ("");

		double dblS0 = 50.;
		double dblX = 1000000.;
		double dblT = 5.;
		double dblAnnualVolatility = 1.00;
		double dblAnnualReturns = 0.10;
		double dblBidAsk = 0.125;
		double dblDailyVolume = 5.e06;
		double dblDailyVolumePermanentImpact = 0.1;
		double dblDailyVolumeTemporaryImpact = 0.01;
		double dblLambdaU = 1.5e-06;

		int iN = 20;

		double dblTime = 0.;
		double dblTimeWidth = dblT / iN;

		ArithmeticPriceDynamicsSettings apds = ArithmeticPriceDynamicsSettings.FromAnnualReturnsSettings (
			dblAnnualReturns,
			dblAnnualVolatility,
			0.,
			dblS0
		);

		double dblSigma = apds.epochVolatility();

		PriceMarketImpactLinear pmil = new PriceMarketImpactLinear (
			new AssetTransactionSettings (
				dblS0,
				dblDailyVolume,
				dblBidAsk
			),
			dblDailyVolumePermanentImpact,
			dblDailyVolumeTemporaryImpact
		);

		ParticipationRateLinear prlPermanent = (ParticipationRateLinear) pmil.permanentTransactionFunction();

		ParticipationRateLinear prlTemporary = (ParticipationRateLinear) pmil.temporaryTransactionFunction();

		LinearPermanentExpectationParameters lpep = ArithmeticPriceEvolutionParametersBuilder.LinearExpectation (
			new ArithmeticPriceDynamicsSettings (
				0.,
				new FlatUnivariate (dblSigma),
				0.
			),
			new UniformParticipationRateLinear (prlPermanent),
			new UniformParticipationRateLinear (prlTemporary)
		);

		ContinuousAlmgrenChriss cac = ContinuousAlmgrenChriss.Standard (
			dblX,
			dblT,
			lpep,
			dblLambdaU
		);

		EfficientTradingTrajectoryContinuous ettcA2012S = (EfficientTradingTrajectoryContinuous) cac.generate();

		R1ToR1 r1ToR1HoldingsA2012S = ettcA2012S.holdings();

		R1ToR1 r1ToR1TradeRateA2012S = ettcA2012S.tradeRate();

		R1ToR1 r1ToR1TransactionCostA2012S = ettcA2012S.transactionCostExpectationFunction();

		ContinuousHighUrgencyAsymptote huas = ContinuousHighUrgencyAsymptote.Standard (
			dblX,
			dblT,
			lpep,
			dblLambdaU
		);

		EfficientTradingTrajectoryContinuous ettcHUAS = (EfficientTradingTrajectoryContinuous) huas.generate();

		R1ToR1 r1ToR1HoldingsHUAS = ettcHUAS.holdings();

		R1ToR1 r1ToR1TradeRateHUAS = ettcHUAS.tradeRate();

		R1ToR1 r1ToR1TransactionCostHUAS = ettcHUAS.transactionCostExpectationFunction();

		System.out.println ();

		System.out.println ("\t|-------------------------------------------------------------||");

		System.out.println ("\t|  HIGH URGENCY vs. ALMGREN 2012 STATIC TRAJECTORY COMPARISON ||");

		System.out.println ("\t|-------------------------------------------------------------||");

		System.out.println ("\t|    L -> R:                                                  ||");

		System.out.println ("\t|            - Time                                           ||");

		System.out.println ("\t|            - Almgren 2012 Holdings                          ||");

		System.out.println ("\t|            - Almgren 2012 Trade Rate                        ||");

		System.out.println ("\t|            - Almgren 2012 Transaction Cost                  ||");

		System.out.println ("\t|            - High Urgency Asymptote Holdings                ||");

		System.out.println ("\t|            - High Urgency Asymptote Trade Rate              ||");

		System.out.println ("\t|            - High Urgency Asymptote Transaction Cost        ||");

		System.out.println ("\t|-------------------------------------------------------------||");

		for (int i = 1; i <= iN; ++i) {
			dblTime = dblTime + dblTimeWidth;

			System.out.println (
				"\t|" +
				FormatUtil.FormatDouble (dblTime, 1, 2, 1.) + " => " +
				FormatUtil.FormatDouble (r1ToR1HoldingsA2012S.evaluate (dblTime) / dblX, 1, 3, 1.) + " | " +
				FormatUtil.FormatDouble (r1ToR1TradeRateA2012S.evaluate (dblTime) * dblTimeWidth / dblX, 1, 3, 1.) + " | " +
				FormatUtil.FormatDouble (r1ToR1TransactionCostA2012S.evaluate (dblTime) / dblX, 1, 3, 1.) + " | " +
				FormatUtil.FormatDouble (r1ToR1HoldingsHUAS.evaluate (dblTime) / dblX, 1, 3, 1.) + " | " +
				FormatUtil.FormatDouble (r1ToR1TradeRateHUAS.evaluate (dblTime) * dblTimeWidth / dblX, 1, 3, 1.) + " | " +
				FormatUtil.FormatDouble (r1ToR1TransactionCostHUAS.evaluate (dblTime) / dblX, 1, 3, 1.) + " ||"
			);
		}

		System.out.println ("\t|-------------------------------------------------------------||");
	}
}
