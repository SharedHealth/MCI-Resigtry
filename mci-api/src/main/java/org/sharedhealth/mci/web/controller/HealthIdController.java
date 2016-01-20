package org.sharedhealth.mci.web.controller;

import org.sharedhealth.mci.web.infrastructure.security.UserInfo;
import org.sharedhealth.mci.web.model.GeneratedHIDBlock;
import org.sharedhealth.mci.web.model.MciHealthId;
import org.sharedhealth.mci.web.service.HealthIdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
@RequestMapping("/healthIds")
public class HealthIdController extends MciController {
    private static final Logger logger = LoggerFactory.getLogger(HealthIdController.class);
    public static final String GENERATE_ALL_URI = "/generate";
    public static final String GENERATE_BLOCK_URI = "/generateBlock";
    public static final String GENERATE_BLOCK_FOR_ORG_URI = "/generateBlockForOrg";
    public static final String NEXT_BLOCK_URI = "/nextBlock";

    private HealthIdService healthIdService;

    @Autowired
    public HealthIdController(HealthIdService healthIdService) {
        this.healthIdService = healthIdService;
    }

    @PreAuthorize("hasAnyRole('ROLE_MCI Admin')")
    @RequestMapping(method = POST, value = GENERATE_ALL_URI)
    public DeferredResult<String> generate() {
        UserInfo userInfo = getUserInfo();

        logAccessDetails(userInfo, "Generating new hids");
        GeneratedHIDBlock generatedHIDBlock = healthIdService.generateAll(userInfo);
        final DeferredResult<String> deferredResult = new DeferredResult<>();
        String message = String.format("Generated %s HIDs.", generatedHIDBlock.getTotalHIDs());
        deferredResult.setResult(message);
        logger.info(message);
        return deferredResult;
    }

    @PreAuthorize("hasAnyRole('ROLE_MCI Admin')")
    @RequestMapping(method = POST, value = GENERATE_BLOCK_URI)
    public DeferredResult<String> generateBlock(@RequestParam(value = "start") long start,
                                                @RequestParam(value = "totalHIDs") long totalHIDs) {
        UserInfo userInfo = getUserInfo();
        logAccessDetails(userInfo, "Generating new hids");
        GeneratedHIDBlock generatedHIDBlock = healthIdService.generateBlock(start, totalHIDs, userInfo);
        return getResult(generatedHIDBlock, totalHIDs);
    }

    @PreAuthorize("hasAnyRole('ROLE_MCI Admin')")
    @RequestMapping(method = POST, value = GENERATE_BLOCK_FOR_ORG_URI)
    public DeferredResult<String> generateBlockForOrg(@RequestParam(value = "org") String orgCode,
                                                      @RequestParam(value = "start") long start,
                                                      @RequestParam(value = "totalHIDs") long totalHIDs) {
        UserInfo userInfo = getUserInfo();
        logAccessDetails(userInfo, "Generating new hids");
        GeneratedHIDBlock generatedHIDBlock = healthIdService.generateBlockForOrg(start, totalHIDs, orgCode, userInfo);
        return getResult(generatedHIDBlock, totalHIDs);
    }

    @PreAuthorize("hasAnyRole('ROLE_MCI Admin')")
    @RequestMapping(method = GET, value = NEXT_BLOCK_URI)
    public List<MciHealthId> nextBlock() {
        return healthIdService.getNextBlock();
    }

    private DeferredResult<String> getResult(GeneratedHIDBlock generatedHIDBlock, long totalHIDs) {
        final DeferredResult<String> deferredResult = new DeferredResult<>();
        String message;
        if (generatedHIDBlock.getTotalHIDs() < totalHIDs) {
            message = String.format("Can generate only %s HIDs, because series exhausted. Use another series.", generatedHIDBlock.getTotalHIDs());
        } else {
            message = String.format("Generated %s HIDs.", generatedHIDBlock.getTotalHIDs());
        }
        deferredResult.setResult(message);
        logger.info(message);
        return deferredResult;
    }
}
