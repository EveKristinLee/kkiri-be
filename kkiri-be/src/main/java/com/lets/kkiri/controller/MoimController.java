package com.lets.kkiri.controller;

import com.lets.kkiri.common.exception.ErrorCode;
import com.lets.kkiri.common.exception.KkiriException;
import com.lets.kkiri.common.util.JwtTokenUtil;
import com.lets.kkiri.common.util.S3Util;
import com.lets.kkiri.dto.MoimGroupPostReq;
import com.lets.kkiri.dto.ReceiptOcrRes;
import com.lets.kkiri.dto.moim.*;
import com.lets.kkiri.service.MoimService;
import com.lets.kkiri.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@RestController
@RequestMapping("/api/moims")
@RequiredArgsConstructor
public class MoimController {
    private final MoimService moimService;
    private final PaymentService paymentService;
    private final S3Util s3Util;


    @PostMapping()
    public ResponseEntity<MoimRegisterRes> moinAdd(
            @RequestHeader(JwtTokenUtil.HEADER_STRING) String accessToken,
            @RequestBody MoimPostReq moimPostReq
    ) {
        String kakaoId = JwtTokenUtil.getIdentifier(accessToken);
        Long moimId = moimService.addMoim(kakaoId, moimPostReq);
        MoimRegisterRes res = MoimRegisterRes.builder().moimId(moimId).build();
        return ResponseEntity.ok().body(res);
    }

    @GetMapping()
    public ResponseEntity<MoimCardListGetRes> moimCardList(
            @RequestHeader(JwtTokenUtil.HEADER_STRING) String accessToken,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
    ) {
        String kakaoId = JwtTokenUtil.getIdentifier(accessToken);

        List<MoimCardDto> moimCards = moimService.findMoimsByKakaoId(kakaoId, date);

        return ResponseEntity.ok().body(MoimCardListGetRes.builder()
                .moimCardList(moimCards)
                .build());
    }

    @GetMapping("/{moimId}")
    public ResponseEntity<MoimInfoGetRes> moimDetail(
            @PathVariable Long moimId
    ) {
        MoimInfoGetRes res = moimService.findMoimById(moimId);
        return ResponseEntity.ok().body(res);
    }

    @PostMapping("/links")
    public ResponseEntity<?> moimLinkAdd(
            @RequestBody MoimLinkPostReq moimPostReq

    ) {
        try {
            moimService.addLinkToMoim(moimPostReq);
        } catch (KkiriException e) {
            throw e;
        } catch (Exception e) {
            throw new KkiriException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/groups")
    public ResponseEntity<MoimRegisterRes> moimGroupJoin(
            @RequestHeader(JwtTokenUtil.HEADER_STRING) String accessToken,
            @RequestBody MoimGroupPostReq moimGroupPostReq
    ) {
        String kakaoId = JwtTokenUtil.getIdentifier(accessToken);
        Long moimId = moimService.addMemberToMoim(kakaoId, moimGroupPostReq);
        MoimRegisterRes res = MoimRegisterRes.builder().moimId(moimId).build();
        return ResponseEntity.ok().body(res);
    }

    @PostMapping("/payment/receipt/ocr")
    public ResponseEntity<?> readReceipt(
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        try {
            String fileName = file.getOriginalFilename();
            fileName = String.valueOf(System.currentTimeMillis()).concat(fileName);
            String contentType = Files.probeContentType(Path.of(fileName));
            if (contentType.startsWith("image")) {   // image
                ReceiptOcrRes receiptOcrRes = paymentService.readReceipt(file);
                String receiptUrl = s3Util.upload(file, "receipt/"+fileName, contentType, file.getSize());
                receiptOcrRes.setReceiptUrl(receiptUrl);
                return ResponseEntity.ok(receiptOcrRes);
            }
        } catch (NoSuchElementException | IOException e){
            log.debug(e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (NullPointerException e) {
            throw new KkiriException(ErrorCode.INVALID_PARAMETER, e.getMessage());
        }
        return ResponseEntity.internalServerError().build();
    }

    @PostMapping("/{moimId}/payment/receipt")
    public ResponseEntity<URI> addReceiptToMoim(
            @PathVariable Long moimId,
            @RequestBody MoimReceiptPostReq moimReceiptPostReq
    ) {
        paymentService.addReceiptToMoim(moimId, moimReceiptPostReq);
        return ResponseEntity.created(URI.create("/api/moims/"+ moimId.toString() +"/payment")).build();
    }

    @GetMapping("/{moimId}/payment")
    public ResponseEntity<MoimPaymentGetRes> getMoimPayment(
            @PathVariable Long moimId
    ) {
        return ResponseEntity.ok().body(MoimPaymentGetRes.builder()
                        .totalExpenditure(paymentService.getMoimExpense(moimId))
                        .totalMemberCnt(moimService.findMemberCountByMoimId(moimId))
                .moimPaymentList(paymentService.getMoimPayment(moimId))
                .build());
    }
}
