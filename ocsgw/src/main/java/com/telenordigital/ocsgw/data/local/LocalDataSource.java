package com.telenordigital.ocsgw.data.local;

import com.telenordigital.ocsgw.OcsServer;
import com.telenordigital.ocsgw.data.DataSource;
import com.telenordigital.ostelco.diameter.CreditControlContext;
import com.telenordigital.ostelco.diameter.model.CreditControlAnswer;
import com.telenordigital.ostelco.diameter.model.FinalUnitAction;
import com.telenordigital.ostelco.diameter.model.FinalUnitIndication;
import com.telenordigital.ostelco.diameter.model.MultipleServiceCreditControl;
import com.telenordigital.ostelco.diameter.model.RedirectAddressType;
import com.telenordigital.ostelco.diameter.model.RedirectServer;
import com.telenordigital.prime.ocs.CreditControlRequestType;
import org.jdiameter.api.IllegalDiameterStateException;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.OverloadException;
import org.jdiameter.api.RouteException;
import org.jdiameter.api.cca.ServerCCASession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Local DataSource that will accept all Credit Control Requests
 * Can be used as a bypass.
 *
 */
public class LocalDataSource implements DataSource {

    private static final Logger LOG = LoggerFactory.getLogger(LocalDataSource.class);

    @Override
    public void init() {
        // No init needed
    }

    @Override
    public void handleRequest(CreditControlContext context) {
        CreditControlAnswer answer = createCreditControlAnswer(context);
        LOG.info("Sending Credit-Control-Answer");
        try {
            final ServerCCASession session = OcsServer.getInstance().getStack().getSession(context.getSessionId(), ServerCCASession.class);
            session.sendCreditControlAnswer(context.createCCA(answer));
        } catch (InternalException | IllegalDiameterStateException | RouteException | OverloadException e) {
            LOG.error("Failed to send Credit-Control-Answer. SessionId : {}", context.getSessionId());
        }
    }

    private CreditControlAnswer createCreditControlAnswer(CreditControlContext context) {

        final List<MultipleServiceCreditControl> origMultipleServiceCreditControls = context.getCreditControlRequest().getMultipleServiceCreditControls();
        final List<MultipleServiceCreditControl> newMultipleServiceCreditControls = new ArrayList<>();

        for (MultipleServiceCreditControl mscc : origMultipleServiceCreditControls) {

            FinalUnitIndication finalUnitIndication = null;

            if (context.getOriginalCreditControlRequest().getRequestTypeAVPValue() == CreditControlRequestType.TERMINATION_REQUEST.getNumber()) {
                finalUnitIndication = new FinalUnitIndication(
                        FinalUnitAction.TERMINATE,
                        new ArrayList<>(),
                        new ArrayList<>(),
                        new RedirectServer(RedirectAddressType.IPV4_ADDRESS));
            }

            MultipleServiceCreditControl newMscc = new MultipleServiceCreditControl(
                    mscc.getRatingGroup(),
                    mscc.getServiceIdentifier(),
                    mscc.getRequestedUnits(),
                    mscc.getUsedUnitsTotal(),
                    mscc.getUsedUnitsInput(),
                    mscc.getUsedUnitsOutput(),
                    mscc.getRequestedUnits(), // granted Service Unit
                    mscc.getValidityTime(),
                    finalUnitIndication);

            newMultipleServiceCreditControls.add(newMscc);
        }

        return new CreditControlAnswer(newMultipleServiceCreditControls);
    }
}
