<?php
class m_booking extends CI_Model{
	var $table 				='tbl_booking';
    
	public function __construct(){
        parent::__construct();
    }

    function _insert($table,$data){
        $this->db->insert($table,$data);
        return $this->db->insert_id();
        
    }

    function getdatamodel($sd,$lokasi){
        $data   =$this->db->select("$this->table.*,jh.*,$this->table.id as bookingid")->from("$this->table")
   
        ->join('tbl_jenis_harga jh',"jh.id=$this->table.jenis_paket","left")
        ->where(array(
		'tgl_booking<='=>$sd,'status'=>'0',"$this->table.lokasi"=>$lokasi))->order_by("$this->table.check_in","asc");
        return $data->get();
    }
    function getlokasi($lokasi){
        return $this->db->get_where('lokasi',array("id"=>$lokasi));
    }
   
    function getdatasales(){
        return $this->db->where('id<>','16')->get('tbl_sales');
    }
    function getdatabank(){
        return $this->db
        ->where('status_pakai','0')
        ->where('status','CR')
        ->get('tbl_bank_upload');
    }
    function getNomorBooking($kodebooking){
        return $this->db->get_where('tbl_booking',array('kode_booking_ref'=>$kodebooking));
    }
    function getdatatamu($identitas){
        $kondisi       ="";
        if($identitas!="1" && $identitas!=""){
            $kondisi = $this->db->like('no_identitas', $identitas);
        }
         $this->db->select('*')->from('tbl_pelanggan');
         $kondisi;
         return $this->db->get();
    }
    function gettypeharga(){
        return $this->db->get('tbl_jenis_harga');
    }
    function _hapus($table,$data){
        $this->db->where($data)
        ->delete($table);
    }
   
    function gettablebyid($id){
        $data   =$this->db->select("$this->table.*,jh.*,$this->table.id as bookingid")->from("$this->table")
   
        ->join('tbl_jenis_harga jh',"jh.id=$this->table.jenis_paket","left")
        ->where(array("$this->table.id"=>$id));
        return $data->get();
    }

    function getleveluserbyid($id){
        $this->db->select('*')
        ->from($this->table)
        ->where('level_id',$id);
        $data   =$this->db->get();
        return $data->row();
    }
    function _updates($data,$where){
        $this->db->where($where)->update($this->table,$data);
        
    }
    function updateTrkas($data,$where){
        $this->db->where($where)->update('tr_kas',$data);
        
    }
    function updateTrAr($data,$where){
        $this->db->where($where)->update('ar_transaction',$data);
        
    }
    function gettyperoom(){
        return $this->db->get('tbl_jenis_room');
    }
   
   function getpembayaran($sales){
    
        $this->db->select('*')->from('tbl_bank_pembayaran');
        if($sales=="OYO"){
          $this->db->where('id','16');
        }
        if($sales=="REDDOORZ"){
          $this->db->where('id','17');
        }
        if($sales=="TIKET.COM"){
          $this->db->where('id','18');
        }
        if($sales=="AGODA"){
          $this->db->where('id','163');
        }
        if($sales=="BOOKING.COM"){
          $this->db->where('id','162');
        }
        if($sales=="OFFLINE"){
         $this->db->where_not_in('id',array('16','17','18','163','162'));
        }
        return $this->db->get();
   }
   function getpembayaranbyid($akunid){
       return $this->db->get_where('tbl_bank_pembayaran',array('id'=>$akunid));
       
   }

   function getdatamodelfiltered($options,$date,$sd,$opsi,$search,$lokasi){
$this->db->select("$this->table.*,jh.*,$this->table.id as bookingid")->from("$this->table")
   
    ->join('tbl_jenis_harga jh',"jh.id=$this->table.jenis_paket");
	if ($options=="periode") {
		if ($date && $sd ) {
			$this->db->where(array('tgl_booking>='=>$date,'tgl_booking<='=>$sd));
           // echo "a";
		}
		if ($opsi) {
			$this->db->like("$opsi",$search);
          //  echo "b";
		}
    }else{
			$this->db->where(array('tgl_booking<='=>$date));
            //echo "c";
    }	
    
$this->db->where("$this->table.lokasi", $lokasi);
    

    $data=$this->db->order_by("$this->table.check_in","asc");
    return $data->get();

   }

   function getdataRoom($sales,$lokasi){
        $this->db->select('*')->from('tbl_room')->where('lokasi',$lokasi)->where('status_room','0')->order_by("nomor_room","ASC");
       // if($sales=="TICKET.COM"){
         //   $this->db->order_by('pri_ticket','ASC');
        //}
        //if($sales=="OYO"){
          //  $this->db->order_by('pri_oyo','ASC');
        //}
        //if($sales=="REDDOORZ"){
          //  $this->db->order_by('pri_reddorz','ASC');
        //}
        //if($sales=="OFFLINE"){
          //  $this->db->order_by('pri_offline','ASC');
        //}
        return $this->db->get();
   }
   function getwheredata($tbl,$dt){
        return $this->db->get_where($tbl,$dt);
   }
   public function getinfobooking($tglcekin,$roomnomor)
   {
    return $this->db->select('detail_booking.*,tbl_jenis_harga.total_jam')
        ->from('detail_booking')
        ->join('tbl_jenis_harga','tbl_jenis_harga.id=detail_booking.jenis_paket','left')
        ->where(array('left(check_in,10)'=>$tglcekin,'room_nomor'=>$roomnomor,'status'=>'0'))->get();
       
   }
   public function getinfocheckin($tglcekin,$roomnomor)
   {
        return 
        $this->db->select('tbl_check_in_detail.*,tbl_sales.nama_sales')
        ->from('tbl_check_in_detail')
        ->join('tbl_sales','tbl_check_in_detail.sales=tbl_sales.id','left')
       
        ->where('status_checkout','0')
        ->where(array('tbl_check_in_detail.room_no'=>$roomnomor,'tbl_check_in_detail.status_checkout'=>'0'))->order_by('tbl_check_in_detail.id','DESC')->get();
   }
   public function getinfocheckout($tglcekin,$roomnomor)
   {
        return 
        $this->db->select('tbl_check_in_detail.*,tbl_sales.nama_sales')
        ->from('tbl_check_in_detail')
        ->join('tbl_sales','tbl_check_in_detail.sales=tbl_sales.id','left')
        ->where(array('left(tbl_check_in_detail.check_out_time,10)'=>$tglcekin,'tbl_check_in_detail.room_no'=>$roomnomor,'tbl_check_in_detail.status_checkout'=>'1'))->get();
   }
   public function getroomKotor($noroom)
   {
     return $this->db->get_where('room_kotor',array('status_cleaning'=>'0','room_no'=>$noroom));
   }
   public function getinfocleaning($tglcekin,$roomnomor)
   {
        return 
        $this->db->select('*')
        ->from('room_cleaning')
        
        ->where(array('room_cleaning.no_room'=>$roomnomor,'room_cleaning.end_time'=>'00:00:00','jenis'=>'C'))->get();
   }
   public function getinfomaintenance($tglcekin,$roomnomor)
   {
        return 
        $this->db->select('*')
        ->from('room_cleaning')
        
        ->where(array('room_cleaning.no_room'=>$roomnomor,'room_cleaning.end_time'=>'00:00:00','jenis'=>'M'))->get();
   }
   public function getinfocleaning2($roomno,$tglcekin)
   {
        return 
        $this->db->select('*')
        ->from('room_cleaning')
        
        ->where(array("concat(room_cleaning.tgl_cleaning,' ','room_cleaning.start_time')>"=>$tglcekin,'room_cleaning.no_room'=>$roomno))->get();
   }
   public function getdatabooking($noroom,$tglroom)
   {
    return $this->db->get_where('tbl_booking',array('left(check_in,10)'=>substr($tglroom,0,10),'room_nomor'=>$noroom,'status'=>'0'));
     
   }
   public function gettables($table)
   {
     return $this->db->get($table);
    
   }
   public function getkasList($refakun)
   {
        return $this->db->get_where('tr_kas',array('left(no_transaksi,2)'=>'DP','ref_akun'=>$refakun));
    # code...
   }
   public function getkota()
   {
    # code...
    return $this->db->get('cities');
   }
   function getwh($tbl,$wh){
    return $this->db->select('*')->from($tbl)->like($wh)->get();
       // return $this->db->get_where($tbl,$wh);
   }
   public function getroomboytrakhir($noroom)
   {
    return $this->db->select('*')->from('room_cleaning')->where(array('no_room'=>$noroom,'status'=>'S'))->order_by('id','DESC')->get();
    # code...
   }

   function getdataBookingbw($bookingid){
     return $data = $this->db->select('*,booking_header.id as bookingid')
     ->from('booking_header')
     ->join('booking_detail','booking_header.id=booking_detail.bookingh_id')
     ->join('tbl_user_web','tbl_user_web.user_wa=booking_header.booking_hp')
     ->where('booking_header.id',$bookingid)->get();

   }
}
