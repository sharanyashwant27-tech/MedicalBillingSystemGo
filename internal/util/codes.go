package util

import (
	"fmt"
	"sync/atomic"
	"time"
)

var counters struct {
	medicine uint64
	bill     uint64
	returnN  uint64
	order    uint64
	entry    uint64
}

func GenerateMedicineCode() string {
	n := atomic.AddUint64(&counters.medicine, 1)
	return fmt.Sprintf("MED-%06d", n)
}

func GenerateBillNumber() string {
	n := atomic.AddUint64(&counters.bill, 1)
	return fmt.Sprintf("BILL-%s-%04d", time.Now().Format("20060102"), n)
}

func GenerateReturnNumber() string {
	n := atomic.AddUint64(&counters.returnN, 1)
	return fmt.Sprintf("RET-%s-%04d", time.Now().Format("20060102"), n)
}

func GenerateOrderNumber() string {
	n := atomic.AddUint64(&counters.order, 1)
	return fmt.Sprintf("ORD-%s-%04d", time.Now().Format("20060102"), n)
}

func GenerateEntryNumber() string {
	n := atomic.AddUint64(&counters.entry, 1)
	return fmt.Sprintf("JE-%s-%04d", time.Now().Format("20060102"), n)
}

func ParseDate(s string) (*time.Time, error) {
	if s == "" {
		return nil, nil
	}
	t, err := time.Parse("2006-01-02", s)
	if err != nil {
		return nil, err
	}
	return &t, nil
}

func Ptr[T any](v T) *T { return &v }

func DerefFloat(p *float64, def float64) float64 {
	if p == nil {
		return def
	}
	return *p
}

func DerefInt(p *int, def int) int {
	if p == nil {
		return def
	}
	return *p
}
