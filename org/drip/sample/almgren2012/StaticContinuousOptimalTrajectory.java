
package org.drip.sample.almgren2012;

import org.drip.execution.dynamics.LinearExpectationParameters;
import org.drip.execution.generator.Almgren2012Static;
import org.drip.execution.impact.ParticipationRateLinear;
import org.drip.execution.optimum.EfficientTradingTrajectoryContinuous;
import org.drip.execution.parameters.*;
import org.drip.function.definition.R1ToR1;
import org.drip.quant.common.FormatUtil;
import org.drip.service.env.EnvManager;

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
 * StaticContinuousOptimalTrajectory demonstrates the Generation and Usage of Continuous Version of the
 *  Discrete Trading Trajectory generated by the Almgren and Chriss (2000) Scheme under the Criterion of
 *  No-Drift. The References are:
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

public class StaticContinuousOptimalTrajectory {

	public static void main (
		final String[] astrArgs)
		throws Exception
	{
		EnvManager.InitEnv ("");

		double dblS0 = 50.;
		double dblX = 1000000.;
		double dblT = 5.;
		double dblAnnualVolatility = 0.30;
		double dblAnnualReturns = 0.10;
		double dblBidAsk = 0.125;
		double dblDailyVolume = 5.e06;
		double dblDailyVolumePermanentImpact = 0.1;
		double dblDailyVolumeTemporaryImpact = 0.01;
		double dblLambdaU = 1.e-06;

		int iN = 20;

		double dblTime = 0.;
		double dblTimeWidth = dblT / iN;

		ArithmeticPriceDynamicsSettings apds = ArithmeticPriceDynamicsSettings.FromAnnualReturnsSettings (
			dblAnnualReturns,
			dblAnnualVolatility,
			0.,
			dblS0
		);

		double dblSigma = apds.volatility();

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

		LinearExpectationParameters lep = new LinearExpectationParameters (
			new ArithmeticPriceDynamicsSettings (
				0.,
				dblSigma,
				0.
			),
			prlPermanent,
			prlTemporary
		);

		Almgren2012Static a2012s = Almgren2012Static.Standard (
			dblX,
			dblT,
			lep,
			dblLambdaU
		);

		EfficientTradingTrajectoryContinuous ettc = (EfficientTradingTrajectoryContinuous) a2012s.generate();

		R1ToR1 r1ToR1Holdings = ettc.holdings();

		R1ToR1 r1ToR1TradeRate = ettc.tradeRate();

		for (int i = 1; i <= iN; ++i) {
			dblTime = dblTime + dblTimeWidth;

			System.out.println (
				"\t|" +
				FormatUtil.FormatDouble (dblTime, 1, 2, 1.) + " => " +
				FormatUtil.FormatDouble (r1ToR1Holdings.evaluate (dblTime) / dblX, 1, 3, 1.) + " | " +
				FormatUtil.FormatDouble (r1ToR1TradeRate.evaluate (dblTime) * dblTimeWidth / dblX, 1, 3, 1.) + " ||"
			);
		}
	}
}
