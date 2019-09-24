//pragma solidity ^0.4.24;

 contract tokenTest{
     grcToken idCon = 0;
     uint256 tokenValueCon=0;
     uint256 callValueCon = 0;

     // positive case
     function TransferTokenTo(address toAddress, grcToken id,uint256 amount) public payable{
         //grcToken id = 0x74657374546f6b656e;
         toAddress.transferToken(amount,id);
     }

     function msgTokenValueAndTokenIdTest() public payable returns(grcToken, uint256, uint256){
         grcToken id = msg.tokenid;
         uint256 tokenValue = msg.tokenvalue;
         uint256 callValue = msg.value;
         return (id, tokenValue, callValue);
     }

     constructor() public payable {
         idCon = msg.tokenid;
         tokenValueCon = msg.tokenvalue;
         callValueCon = msg.value;
     }

     function getResultInCon() public payable returns(grcToken, uint256, uint256) {
         return (idCon, tokenValueCon, callValueCon);
     }
 }