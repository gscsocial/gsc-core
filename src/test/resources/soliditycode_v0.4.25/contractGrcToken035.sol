pragma solidity ^0.4.24;

 contract token{

     // 2. 异常测试
     // 1）revert, 金额回退
     function failTransferTokenRevert(address toAddress,uint256 amount, grcToken id) public payable{
         toAddress.transferToken(amount,id);
         require(1==2);
     }

     // 2）Error, 金额回退， fee limit 扣光
     function failTransferTokenError(address toAddress,uint256 amount, grcToken id) public payable{
         toAddress.transferToken(amount,id);
         assert(1==2);
     }

 }
 contract B{
    uint256 public flag = 0;
    constructor() public payable {}
    function() public payable {}
}