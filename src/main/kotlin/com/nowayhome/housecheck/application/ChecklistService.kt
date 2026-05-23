package com.nowayhome.housecheck.application

import com.nowayhome.housecheck.domain.AnalysisStatus
import com.nowayhome.housecheck.domain.ChecklistStage
import com.nowayhome.housecheck.persistence.BuildingLedgerManualFindingEntity
import com.nowayhome.housecheck.persistence.HouseCheckRequestEntity
import com.nowayhome.housecheck.persistence.MarketPriceSnapshotEntity
import com.nowayhome.housecheck.persistence.RegistryManualFindingEntity
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ChecklistService {
    fun buildChecklist(
        houseCheck: HouseCheckRequestEntity,
        registryFinding: RegistryManualFindingEntity?,
        buildingFinding: BuildingLedgerManualFindingEntity?,
        marketPrice: MarketPriceSnapshotEntity?,
    ): HouseChecklistResponse {
        val beforeContract = mutableListOf(
            "계약 전 등기부등본 최신본을 다시 확인했는지 점검합니다.",
            "임대인과 등기상 소유자 일치 여부를 다시 확인합니다.",
            "보증금과 선순위 채권 합계가 추정 시세 대비 과도하지 않은지 확인합니다.",
        )
        if (registryFinding == null) {
            beforeContract += "등기부등본 수동 확인 결과를 입력했는지 확인합니다."
        }
        if (buildingFinding == null) {
            beforeContract += "건축물대장 수동 확인 결과를 입력했는지 확인합니다."
        }
        if (marketPrice == null) {
            beforeContract += "추정 시세와 기준일, 출처를 기록했는지 확인합니다."
        }

        val rightBeforeContract = mutableListOf(
            "계약 직전 발급일이 최신인지 다시 확인합니다.",
            "다가구·다세대의 경우 선순위 임차인 정보 확인이 필요한지 점검합니다.",
            "보증보험 가입 가능 여부와 필요 서류를 확인합니다.",
        )
        if (houseCheck.analysisStatus != AnalysisStatus.COMPLETED) {
            rightBeforeContract += "현재 확인된 자료 기준 리포트를 다시 생성했는지 확인합니다."
        }

        val afterContract = listOf(
            "계약 후 전입신고와 확정일자 진행 여부를 확인합니다.",
            "원본 계약서와 업로드 문서 보관 위치를 확인합니다.",
            "잔금 이후에도 필요 시 등기 재확인 일정을 남깁니다.",
        )

        return HouseChecklistResponse(
            checkId = houseCheck.id,
            analysisStatus = houseCheck.analysisStatus,
            sections = listOf(
                ChecklistSectionResponse(ChecklistStage.BEFORE_CONTRACT, "계약 전", beforeContract),
                ChecklistSectionResponse(ChecklistStage.RIGHT_BEFORE_CONTRACT, "계약 직전", rightBeforeContract),
                ChecklistSectionResponse(ChecklistStage.AFTER_CONTRACT, "계약 후", afterContract),
            ),
        )
    }
}
