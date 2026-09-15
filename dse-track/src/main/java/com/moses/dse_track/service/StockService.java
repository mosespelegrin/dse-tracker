package com.moses.dse_track.service;

import com.moses.dse_track.dto.request.StockFundamentalsRequest;
import com.moses.dse_track.exception.BusinessException;
import com.moses.dse_track.model.Stock;
import com.moses.dse_track.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockService {
    private final StockRepository stockRepository;
    //for displaying stocks when user enters data for his/ her new stock
    public List<Stock> getAllStocks(){
        return stockRepository.findAll();
    }
    //used by other services internally example holding and alert
    public Stock getByTicker(String ticker){
        return stockRepository.findByTicker(ticker)
                .orElseThrow(()->new BusinessException("stock not found"+ ticker));
    }
    public Stock getById(Long id){
        return stockRepository.findById(id)
                .orElseThrow(()->new BusinessException(" stock not found"));
    }

    public Stock updateFundamentals(Long id, StockFundamentalsRequest request) {
        Stock stock = getById(id);
        stock.setEpsTtm(request.getEpsTtm());
        stock.setSharesOutstanding(request.getSharesOutstanding());
        stock.setNetIncome(request.getNetIncome());
        stock.setTotalEquity(request.getTotalEquity());
        stock.setTotalAssets(request.getTotalAssets());
        stock.setTotalLiabilities(request.getTotalLiabilities());
        stock.setCurrentAssets(request.getCurrentAssets());
        stock.setCurrentLiabilities(request.getCurrentLiabilities());
        stock.setAnnualDividendPerShare(request.getAnnualDividendPerShare());
        stock.setFundamentalsUpdatedAt(LocalDateTime.now());
        return stockRepository.save(stock);
    }
}
